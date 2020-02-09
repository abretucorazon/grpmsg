(ns grpmsg.db
  ;; (:require [clojure.spec.alpha :as s])
  )

;; spec of app-db
;;(s/def ::greeting string?)
;;(s/def ::app-db
;;  (s/keys :req-un [::greeting]))

;; initial state of app-db
(def app-db {:greeting "grpmsg"
             :messages [{:user "Greeting" :text "Welcome to ClojureScript Chat App"}
                        {:user "John" :text "Hello Mike" }
                        {:user "Mike" :text "Hi John" }] })
