(ns cljrx.core
  :import java.io.Closable)

(defprotocol IObservable 
  (subscribe [this ^IObserver observer] 
             "Registers the observer with the observable. Returns a function  
              that when called, unregisters the Observer"))

(defprotocol IObserver
    (onNext [this evt] "Called when a new event arrives")
    (onError [this err] "Called when an error occurs")
    (onEnd [this] "Called when the stream ends"))


(defprotocol IObservableLookup
    (to-observable [this key] "Returns a IObservable based on the key"))


(defrecord Mapobservable [parentrefcloser f subscribers]
  IObservable
  (subscribe [this ^IObserver observer] 
      (swap!  subscribers conj observer)
      #((swap! subscribers disj observer)))
  IObserver
  (onNext [this evt] 
    (let [newresult (f evt)]
      (map #( try (onNext % newresult)
               (catch Exception e  (onError % e))
                )   @subscribers))  ) 
  (onError [this err] 
    (map #(onError % err)  @subscribers)) 
  (onEnd [this]
    (map  onEnd   @subscribers)
    (close this)) 
  Closable
  (close (map  #(.close %)  @subscribers) 
    ;(reset!  subscribers)
    (if refcloser (refcloser))))
(defn rmap [f ^IObservable observable]
  (let [item (Mapobservable. nil f #{})]
    (assoc item :parentrefcloser (subscribe observable item )) 
    )
  )