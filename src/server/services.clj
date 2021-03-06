(ns server.services
  (:require [clojure.core.strint :refer [<<]]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [ring.util.response :refer [response]]))

;; Good interfaces for gene and protein data:
;;
;; - TogoWS REST: http://togows.dbcls.jp/site/en/rest.html
;; - Ensembl: http://beta.rest.ensembl.org/documentation
;;
;; These are backed by sources like NCBI databases, KEGG

(def base-url 
  "http://beta.rest.ensembl.org/sequence/region")

(defn rand-int-upto [n]
  (-> (rand)
    (* n)
    int))

(defn randomize [choices n]
  (->>
    (repeatedly #(choices (rand-int-upto (count choices)))) 
    (take n)))

;; Assumes Eukaryote with chromosomes for now
(defn fetch-gene-data [species-id chromosome start-pos len]
  ;; Range should be something like human/X:1000000..1000100
  ;; See http://beta.rest.ensembl.org/documentation/info/sequence_region
  ;; << is string interpolation, provided by clojure.incubator
  (let [end-pos (+ start-pos len)
        url (<< "~{base-url}/~{species-id}/~{chromosome}:~{start-pos}..~{end-pos}")
        resp (http/get url {:headers {"Content-Type" "application/json"}
                            :throw-exceptions false})
        body (json/decode (resp :body) true)]
    (if body
      {:type :actual-data
       :sequence (body :seq)}
      {:type :random-data
       :sequence (randomize ["A" "C" "T" "G"] len)})))
