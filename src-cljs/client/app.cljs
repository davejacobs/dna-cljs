(ns client.app
  (:require [client.helpers :as h]
            [clojure.string :refer [upper-case]]
            [cljs.core.async :refer [<! >! put! close! timeout]]
            [chord.client :refer [ws-ch]]
            [jayq.core :refer [$ css html] :as jq]
            [dommy.core :as dommy])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [clojure.core.strint :refer [<<]]
                   [dommy.macros :refer [deftemplate node sel sel1]] 
                   [jayq.macros :refer [ready]]))

(def counter
  (atom 0))

(def application-state 
  (atom {:sequences {}}))

(def reading-state 
  (atom {:sequences {}}))

(deftemplate sequence-templ [identifier]
  [:div.sequence-wrapper
   [:ul {:class identifier}]
   [:a {:href "#"
        :class "pause-fetch"
        :data-identifier identifier}
    "Pause"]])

(deftemplate nucleotide-templ [base]
  [:li
   {:classes [base "nucleotide"]}
   base])

(defn nucleotides-templ [sequence]
  (map nucleotide-templ sequence))

(defn update-sequence! [identifier old-seq new-seq]
  (let [identifier-name (name identifier)
        seq-diff (drop (count old-seq) new-seq)
        elem ($ (<< ".~{identifier-name}"))]
    (doseq [nucleotide (nucleotides-templ seq-diff)]
      (jq/append elem nucleotide))))

(defn insert-sequence! [identifier new-seq]
  (let [identifier-name (name identifier)]
    (-> ($ ".sequences")
      (jq/append (sequence-templ identifier-name)))
    (-> ($ ".pause-fetch")
      (jq/on :click on-pause-fetch))
    (update-sequence! identifier [] new-seq)))

(defn render! [old-state new-state]
  (doseq [[identifier new-seq] (new-state :sequences)]
    (if-let [old-seq ((old-state :sequences) identifier)]
      (update-sequence! identifier old-seq new-seq)
      (insert-sequence! identifier new-seq))))

(defn render-search-sequence! [search-sequence]
  (doseq [[identifier sequence] (@application-state :sequences)]
    (let [identifier-name (name identifier)
          search-positions (h/find-pos sequence search-sequence) 
          highlight-len (count search-sequence)] 
      (doseq [initial-pos search-positions
              distance (range highlight-len)]
        (-> ($ (<< ".~{identifier-name} > li"))
          (.eq (+ initial-pos distance))
          (jq/add-class "highlighted"))))))
 
(defn start-loading-data! [query]
  ;; Bump the counter
  (swap! counter inc)
  (let [identifier (keyword (str "sequence-" @counter))]
    ;; Allot a new place in state for this sequence
    (swap! application-state assoc-in [:sequences identifier] []) 
    (swap! reading-state assoc-in [:sequences identifier] true) 
    ;; Request the data
    (go
      (let [ws (<! (ws-ch "ws://localhost:8080/data"))]
        (>! ws query)
        ;; Read all data off of queue
        (while true
          (if (get-in @reading-state [:sequences identifier])  
            (let [next-queue-item (<! ws)
                  message (next-queue-item :message)]
              (swap! application-state 
                     update-in [:sequences identifier] #(concat % message)))
            (<! (timeout 500))))))))

(defn start-loading-data-from-form! [form fields]
  (let [query (h/form->map form fields)]
    (start-loading-data! query)))

(defn fit-input-width! [input-elem]
  (let [letter-width 10
        min-chars 1
        input-val (str (jq/val input-elem))
        input-val-len (count input-val)
        placeholder (-> input-elem (jq/attr "placeholder"))
        placeholder-len (count placeholder)
        len (cond
              (>= input-val-len min-chars) input-val-len
              (and placeholder (>= placeholder-len min-chars)) placeholder-len
              :else min-chars)]
    (jq/width input-elem (* len letter-width))))

(defn on-input-text [e]
  (let [letter-width 10
        min-chars 5
        target ($ (.-target e))]
    (fit-input-width! target)))

(defn on-fetch-sequences [e]
  (jq/prevent e)
  (let [form (.-currentTarget e)
        fields [:species-id :chromosome :start-pos :len]]
    (h/save-form-to-cookie! form fields)
    (start-loading-data-from-form! form fields)))

(defn on-search-sequences [e]
  (jq/prevent e)
  (let [form (.-currentTarget e)
        search-val (h/form-value-for-name form "search-sequence")
        search-seq (seq (upper-case search-val))]
    (render-search-sequence! search-seq)
    #_(-> ($ form) (jq/find "[name=search-sequence]") (jq/val "") (jq/focus))))

(defn on-pause-fetch [e]
  (jq/prevent e)
  (let [target ($ (.-target e))
        identifier (keyword (jq/data target "identifier"))]
    (swap! reading-state assoc-in [:sequences identifier] false)))

(defn bind-events! []
  (-> ($ "form.fetch-sequences")
    (jq/bind :submit on-fetch-sequences))
  (-> ($ "form.search-sequences")
    (jq/bind :submit on-search-sequences))
  #_(-> ($ "input[type=text]")
    (jq/bind :keyup on-input-text)))

(defn init! []
  (enable-console-print!)
  (h/load-form-from-cookie! ($ "form")
                            ["species-id" "chromosome" "start-pos" "len"])
  (add-watch application-state :app-watcher
             (fn [key reference old-state new-state]
               (render! old-state new-state)))
  (bind-events!)
  #_(fit-input-width! ($ "input[type=text]")))

(jq/document-ready init!)
