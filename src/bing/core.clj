(ns bing.core
  (:gen-class :main true)
  (:require [clj-http.client :as client])
  (:require [clojure.data.json :as json])
  (:require [clojure.string :as string])
  (:require [clojure.walk :as walk])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [clojure.data.csv :as csv])
  (:require [clojure.java.io :as io]))


;; lein run example_data/regions.csv hg38 example_data/PolII.csv example_data/PolII_custom_tracks.txt data/ out.pdf 'endrebak85@gmail.com'

;; (def cli-options
;;   [["-h" "--help"]
;;    ["-v" "--version"]
;;    ["-e" "--e-mail EMAIL" "E-mail used to tell UCSC who you are."]  ;; :validate [#(re-matches #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?]" %)]]
;;    ["-g" "--genome GENOME" "Genome version"
;;     :default "hg38"]
;;    ["-r" "--regions REGIONS" "File with regions to graph" :default nil]
;;    ["-u" "--ucsc-tracks UCSC" "File with UCSC tracks"]
;;    ["-c" "--custom-tracks CUSTOM" "File with custom tracks"]
;;    ["-f" "--outfolder OUTFOLDER" "Folder to place region pdfs in" :default "data/"]
;;    ["-o" "--outfile OUTFILE" "Final pdf-report"]
;;    ])


(defn request [q fields species scopes]
  "Request data from mygene.info"
  (client/post "http://mygene.info/v3/query" {:form-params
                                              {:q q :fields fields :species species :scopes scopes}}))

(defn parsed-response [response]
  "Turn response string into json dict"
  (->>
   (:body response)
   (json/read-str)
   (map #(walk/keywordize-keys %))))


(defn fetch-fields [d keyword-fields]
  "Helper methods for fetching fields that are nested."
  (for [field keyword-fields]
    (if (= field :ensembl)
      [field (get-in d [:ensembl :gene])]
      [field (get-in d [field])])))


(defn subsetted-result [fields ds]
  "Fetch the appropriate fields from the result dicts"
  (let [keyword-fields (map keyword (string/split fields #","))]
    (for [d ds]
      (into {} (fetch-fields d keyword-fields)))))


(defn output-results [entries]
  "Write result to stdout"
  (let [values (map #(clojure.string/join "\t" (vals %)) entries)
        allrows (clojure.string/join "\n" values)]
    (print allrows)))


(defn parse-query [v]
  "If a comma-separated string do nothing, otherwise read as path and turn into
  comma-separated string."
  (if (re-find #"," v)
    v)
    (let [file-contents (slurp v)]
      (clojure.string/replace file-contents "\n" ",")))


(defn main [q fields species scopes]
  "Fetch and parse results from mygene.info"
  (let [q (parse-query)]
    (->>
     (request q fields species scopes)
     (parsed-response)
     (subsetted-result fields)
     (output-results))))


;; (def example-q "CDK2,CDK3")
;; (def example-fields "symbol,name,entrezgene,ensembl")
;; (def example-species "human")
;; (def example-scopes "symbol,name,entrezgene,ensembl")

;; example run:
; (main example-q example-fields example-species example-scopes)
; (request example-q example-fields example-species example-scopes)

;; (require '[clojure.data.csv :as csv]
;;          '[clojure.java.io :as io])

;; (with-open [reader (io/reader "in-file.csv")]
;;   (doall
;;    (csv/read-csv reader)))

;; (with-open [writer (io/writer "out-file.csv")]
;;   (csv/write-csv writer
;;                  [["abc" "def"]
;;                   ["ghi" "jkl"]]))
