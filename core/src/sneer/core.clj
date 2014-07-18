(ns sneer.core
  (:require [rx.lang.clojure.core :as rx])
  (:import [sneer.admin SneerAdmin]
           [sneer Sneer PrivateKey]
           [sneer.tuples Tuple Tuples TuplePublisher TupleSubscriber]
           [rx.subjects ReplaySubject]))

(defmacro reify+ [iface & body]
  `(~'reify ~iface ~@(apply concat (map #(list (macroexpand-1 %)) body))))

(defmacro tuple-attr [a]
  `(~a [~'this]
       (get ~'attrs ~(name a))))

(defn ->tuple [attrs]
  (reify+ Tuple
    (tuple-attr intent)
    (tuple-attr value)))

(defmacro publisher-attr [a]
  `(~a [~'this ~a]
       (~'with ~(name a) ~a)))

(defn new-tuple-publisher
  ([tuples] (new-tuple-publisher tuples {}))
  ([tuples attrs]
    (letfn
      [(with [attr value]
          (new-tuple-publisher tuples (assoc attrs attr value)))]
      (reify+ TuplePublisher
        (publisher-attr intent)
        (publisher-attr audience)
        (publisher-attr value)
        (pub [this value]
            (.. this (value value) pub))
        (pub [this]
            (. tuples onNext (->tuple attrs))
            this)))))

(defn new-tuple-subscriber [tuples]
  (reify TupleSubscriber
    (intent [this expected]
      (new-tuple-subscriber
        (rx/filter #(= (. % intent) expected) tuples)))
    (tuples [this]
      tuples)))

(defn new-tuples [tuples]
  (reify Tuples
    (newTuplePublisher [this]
      (new-tuple-publisher tuples))
    (newTupleSubscriber [this]
      (new-tuple-subscriber tuples))))

(defn new-sneer-admin [tuples]
  (reify SneerAdmin
    (initialize [this pk]
      (reify Sneer
        (tuples [this]
          (new-tuples tuples))))))

(defn new-session []
  (ReplaySubject/create))