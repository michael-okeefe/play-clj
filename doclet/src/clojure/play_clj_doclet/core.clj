(ns play-clj-doclet.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [marginalia.core :as marg]
            [play-clj-doclet.html :as html])
  (:import [com.sun.javadoc ClassDoc ConstructorDoc Doc ExecutableMemberDoc
            FieldDoc Parameter RootDoc]))

(def targets (-> "targets.edn" io/resource slurp edn/read-string))

(defn camel->keyword
  [s]
  (->> (string/split (string/replace s "_" "-") #"(?<=[a-z])(?=[A-Z])")
       (map string/lower-case)
       (string/join "-")
       keyword))

(defn parse-param
  [^Parameter p]
  [(.typeName p) (-> (.name p) camel->keyword name)])

(defn parse-doc-name
  [^Doc d]
  (cond
    (isa? (type d) ConstructorDoc)
    nil
    (isa? (type d) ClassDoc)
    (subs (.name d) (+ 1 (.lastIndexOf (.name d) ".")))
    :else
    (.name d)))

(defn parse-doc
  [^Doc d]
  (merge {}
         (when-let [n (some-> (parse-doc-name d) camel->keyword)]
           {:name n})
         (when (> (count (.commentText d)) 0)
           {:text (.commentText d)})
         (cond
           (isa? (type d) ExecutableMemberDoc)
           {:args (->> d .parameters (map parse-param) vec)}
           (isa? (type d) FieldDoc)
           {:args [[(-> d .type .typeName) "val"]]})))

(defn parse-class-entry
  [^ClassDoc c type]
  (some->> (case type
             :methods (filter #(-> % .isStatic not) (.methods c))
             :static-methods (filter #(.isStatic %) (.methods c))
             :fields (filter #(-> % .isStatic not) (.fields c))
             :static-fields (filter #(.isStatic %) (.fields c))
             :classes (filter #(-> % .isStatic not) (.innerClasses c))
             :static-classes (filter #(.isStatic %) (.innerClasses c))
             :constructors (filter #(-> % .isStatic not) (.constructors c))
             nil)
           (filter #(.isPublic %))
           (map parse-doc)
           (concat (when-let [sc (.superclass c)]
                     (when (not= (.typeName sc) "Object")
                       (parse-class-entry sc type))))
           vec))

(defn parse-class
  [^ClassDoc c]
  (some->> (get targets (.typeName c))
           (map #(vector (first %) (parse-class-entry c (second %))))
           (into {})))

(def ^:const valid-syms #{'defn 'defmacro})

(defn match?
  [doc-name sym-name]
  (or (= doc-name sym-name)
      (.startsWith doc-name (str sym-name " "))))

(defn process-group
  [{:keys [type raw] :as group} doc-map]
  (let [form (read-string raw)
        n (second form)]
    (when (and (contains? valid-syms (first form))
               (-> n meta :private not))
      (assoc group
             :name (str n)
             :java (filter #(match? (first %) (str n)) doc-map)))))

(defn process-groups
  [{:keys [groups] :as parsed-file} doc-map]
  (->> (map #(process-group % doc-map) groups)
       (remove #(nil? (:name %)))
       (assoc parsed-file :groups)))

(defn parse-clj
  [doc-map]
  (->> (io/file "../src/")
       file-seq
       (filter #(-> % .getName (.endsWith ".clj")))
       (sort-by #(.getName %))
       (map #(.getCanonicalPath %))
       (map marg/path-to-doc)
       (map #(process-groups % doc-map))))

(defn save
  [parsed-files]
  (->> parsed-files pr-str (spit (io/file "uberdoc.edn")))
  (->> parsed-files html/create (spit (io/file "uberdoc.html"))))

(defn parse
  [^RootDoc root]
  (->> (map parse-class (.classes root))
       (filter some?)
       (into {})
       parse-clj
       save)
  (println "Created uberdoc.html and uberdoc.edn."))