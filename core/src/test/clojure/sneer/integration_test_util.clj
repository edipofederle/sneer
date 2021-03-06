(ns sneer.integration-test-util
  (:require [midje.sweet :refer :all]
            [clojure.core.async :refer [close!]]
            [sneer.test-util :refer [emits]]
            [sneer.tuple.jdbc-database :refer [create-sqlite-db]])
  (:import [clojure.lang IFn]
           [sneer.commons Container PersistenceFolder]
           [sneer.impl CoreLoader]
           [java.io Closeable]
           [sneer.flux LeaseHolder]
           [sneer.tuple.protocols Database]))

(defn sneer! []
  (let [delegate (Container. (CoreLoader.))
        transient nil]
    (.inject delegate PersistenceFolder (reify PersistenceFolder (get [_] transient)))
    (.inject delegate Database (create-sqlite-db))

    (reify
      IFn
      (invoke [_ component] (.produce delegate component))

      Closeable
      (close [_] (close! (.getLeaseChannel (.produce delegate LeaseHolder)))))))
