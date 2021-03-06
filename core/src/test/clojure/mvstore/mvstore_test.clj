(ns mvstore.mvstore-test
  (:require [midje.sweet :refer :all])
  (:import
    [org.h2.mvstore MVStore]
    (java.io File)))

(facts "MVStore Works"
  (let [filename (.getAbsolutePath (File/createTempFile "tmp" ".tmp" ))]
    (with-open [store (MVStore/open filename)]
      (let [map1 (.openMap store "map-name")]

        (fact "Map put and get work."
          (.get map1 "key") => nil
          (.put map1 "key" "value")
          (.get map1 "key") => "value")

        (fact "Arrays work as keys"
          (.put map1 (.getBytes "key2") "value2")
          (.get map1 (.getBytes "key2")) => "value2")

        (fact "Serializable objects can be stored"
          (.put map1 "key3" {:a :b})
          (.get map1 "key3") => {:a :b})

        (fact "Serializable objects can be used as keys"
          (.put map1 {:c :d} "value4")
          (.get map1 {:c :d}) => "value4")))


    (with-open [store (MVStore/open filename)]
      (let [map1 (.openMap store "map-name")]

        (fact "Map is persistent"
          (.get map1 "key") => "value"
          (.get map1 (.getBytes "key2")) => "value2"
          (.get map1 "key3") => {:a :b}
          (.get map1 {:c :d}) => "value4")))

    (-> filename File. .delete) => true))