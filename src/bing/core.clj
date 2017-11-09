(ns bing.core
  (:gen-class :main true)
  (:require [clj-http.client :as client])
  (:require [clojure.data.json :as json])
  (:require [clojure.string :as string])
  (:require [clojure.walk :as walk])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [clojure.data.csv :as csv])
  (:require [clojure.java.io :as io]))

(def version "0.0.1")
(def allowed-values #{"symbol" "name" "entrezgene" "ensembl"})

(defn parse-gene-file [f]
  (let [file-contents (slurp f)]
    (clojure.string/replace file-contents "\n" ",")))

(defn parse-args-string [s]
  (->
   (clojure.string/lower-case s)
   (string/split #",")))


(def cli-options
  [["-h" "--help"]
   ["-v" "--version" version]
   ["-g" "--species" "Possible common name values: human, mouse, rat, fruitfly, nematode, zebrafish, thale-cress, frog and pig.
For other species, use a taxonomy id: http://docs.mygene.info/en/latest/doc/query_service.html#" :default "human"]
   ["-q" "--query QUERY" "Comma-separated list of gene names."]
   ["-Q" "--query-file FILE" "Newline-separated list of gene names" :parse-fn parse-gene-file :validate-fn #(.exists (io/as-file %))]
   ["-f" "--fields FIELDS" "What fields to print. Available:
   symbol,name,entrezgene,ensembl."
    :default "symbol,name,entrezgene,ensembl"
    :parse-fn
   parse-args-string :validate [#(every? allowed-values %)
     "Only comma-separated list with values symbol,name,entrezgene,ensembl allowed."]]
   ["-s" "--scopes SCOPES" "What to fields to search for a query match in."
    :default "symbol,name,entrezgene,ensembl"
    :parse-fn parse-args-string :validate
    [#(every? allowed-values %)
     "Only comma-separated list with values symbol,name,entrezgene,ensembl allowed."]]])

;; http://docs.mygene.info/en/latest/doc/query_service.html?highlight=scopes#available-fields

;; :default "symbol,name,entrezgene,ensembl"
(defn request [q fields species scopes]
  "Request data from mygene.info"
  (client/post "http://mygene.info/v3/query" {:form-params {:q q :fields
                                              fields :species species :scopes
                                              scopes}}))


(defn parsed-response [response]
  "Turn response string into json dict"
  (->>
   (:body response)
   (json/read-str)
   (map #(walk/keywordize-keys %))))


(defn fetch-fields [d keyword-fields]
  "Helper methods for fetching fields that are nested."
  (for [field keyword-fields]
    (case field
      :ensembl [field (get-in d [:ensembl :gene])]
      [field (get-in d [field])])))


(defn subsetted-result [fields ds]
  "Fetch the appropriate fields from the result dicts"
  (let [keyword-fields (map keyword (string/split fields #","))]
    (for [d ds]
      (into {} (fetch-fields d keyword-fields)))))


(defn output-results [entries]
  "Write result to stdout"
  (let [rows (map #(clojure.string/join "\t" (vals %)) entries)]
    (doseq [row rows]
      (println row))))


(defn -main [& args]
  "Fetch and parse results from mygene.info"
  (let [{:keys [options arguments summary errors]} (parse-opts args cli-options)]
    (cond
      (seq errors) (binding [*out* *err*]
                     (println (clojure.string/join "\n" errors)))
      (:help options) (print summary)
      (:version options) (print version))
      :default
       (let [{:keys [species query query-file fields scopes]} options]
         (->>
          (request query fields species scopes)
          (parsed-response)
          (subsetted-result fields)
          (output-results)))))



;; (def example-q "CDK2,CDK3")
;; (def example-fields "symbol,name,entrezgene,ensembl")
;; (def example-species "human")
;; (def example-scopes "symbol,name,entrezgene,ensembl")

;; example run:
; (main example-q example-fields example-species example-scopes)
; (request example-q example-fields example-species example-scopes)
