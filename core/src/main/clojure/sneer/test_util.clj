(ns sneer.test-util
  (:require
    [sneer.commons :refer [nvl]]
    [clojure.core.async :refer [alt!! timeout filter> >!! <!! close! chan]]
    [rx.lang.clojure.core :as rx]
    [sneer.async :refer [go-loop-trace]]
    [sneer.rx :refer [observe-for-io subscribe-on-io]])
  (:import [java.io File]))

(defn tmp-file []
  (doto
    (File/createTempFile "test-" ".tmp")
    (.delete)))

(defn tmp-folder []
  (doto
    (File/createTempFile "test-" ".tmp")
    (.mkdir)))

(defn >!!?
  ([ch v]
    (>!!? ch v 200))
  ([ch v timeout-millis]
    (alt!!
      (timeout timeout-millis) false
      [[ch v]] true)))

(defn <!!?
  ([ch]
    (<!!? ch 200))
  ([ch timeout-millis]
    (alt!!
      (timeout timeout-millis) :timeout
      ch ([v] v))))

(defn ->predicate [expected]
  (if (fn? expected) expected #(= % expected)))

(defn <wait-for! [ch expected]
  (let [expected (nvl expected :nil)
        pred (->predicate expected)]
    (<!!
      (go-loop-trace [last-value nil]
        (let [current (<!!? ch)]
          (cond
            (pred current) true
            (nil? current) (do
                             (println "COMPLETED/CLOSED. Last value: " last-value)
                             false)
            (= current :timeout) (do
                                   (println "TIMEOUT. Last value:" last-value)
                                   false)
            :else (recur current)))))))

(defn emits [expected]
  (fn [obs]
    (let [ch (->chan obs)]
      (<wait-for! ch expected))))

(defn compromised
  ([ch] (compromised ch 0.7))
  ([ch failure-rate]
    (filter> (fn [_] (> (rand) failure-rate)) ch)))

(defn compromised-if [unreliable ch]
  (if unreliable
    (compromised ch)
    ch))


(defn subscribe-chan [c observable]
  (rx/subscribe observable
                #(>!! c (nvl % :nil))  ; Channels cannot take nil
                #(do
                   (.printStackTrace %)
                   (close! c))
                #(close! c)))

(defn observable->chan
  ([obs]
   (doto (chan)
     (subscribe-chan obs)))
  ([obs xform]
   (doto (chan 1 xform)
     (subscribe-chan obs))))

(defn ->chan2
  ([obs]
   (observable->chan (subscribe-on-io obs)))
  ([obs xform]
   (observable->chan (subscribe-on-io obs) xform)))

(defn pst [fn]
  (try (fn)
       (catch Exception e (.printStackTrace e))))

(defn ->chan [^rx.Observable o]
  (->> o observe-for-io observable->chan))

; (do (require 'midje.repl) (midje.repl/autotest))