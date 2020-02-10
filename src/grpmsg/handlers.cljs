(ns grpmsg.handlers
  (:require
   [re-frame.core :refer [reg-event-db ->interceptor dispatch dispatch-sync]]
   [clojure.spec.alpha :as s]
   [grpmsg.db :as db :refer [app-db]]
   [grpmsg.firebase-config :refer [firebase-config]]))

;; -- Firebase ----------------------------------------------------------
; Import Firebase SDK
; Initialize Firebase
(defonce fb  (js/require "firebase/app"))
(defonce not-used1 (.initializeApp fb firebase-config))
(defonce not-used2 (js/require "firebase/firestore"))
(defonce db (.firestore fb))
(defonce fb-col-name "messages")
(defonce fb-colref (.collection db fb-col-name))

;; Firebase listener callback
(defn fb-listener [snapshot]
    (print "Firebase snapshot - # of changes is " (-> (.docChanges snapshot) (js->clj :keywordize-keys true) (count)))
    (let [changes  (-> snapshot (.docChanges) (js->clj :keywordize-keys true))
          recv-msgs (map (fn [x] (-> x (:doc) (.data) (js->clj :keywordize-keys true))) changes)
          ]
      (doseq [x recv-msgs]
        (dispatch [:receive-message x])
        )))


;; Setup listener for firebase updates
(def fb-unsubscribe (.onSnapshot fb-colref 
                                 fb-listener
                                 (fn [error] (print "Firebase listener error: " error))))


;; Add new text message to firebase document
(defn fb-add-msg [user msg]
  (-> fb-colref
      (.add #js{:username user :text msg :time (fb.firestore.Timestamp.now)})
      (.then (fn[s] (print "Message added to firebase!" )))
      (.catch (fn [error] (print "Error adding message: " error))))
      )



;; -- Interceptors ----------------------------------------------------------
;;
;; See https://github.com/Day8/re-frame/blob/develop/docs/Interceptors.md
;;
(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db]
  (when-not (s/valid? spec db)
    (let [explain-data (s/explain-data spec db)]
      (throw (ex-info (str "Spec check failed: " explain-data) explain-data)))))

(def validate-spec
  (if goog.DEBUG
    (->interceptor
        :id :validate-spec
        :after (fn [context]
                 (let [db (-> context :effects :db)]
                   (check-and-throw ::db/app-db db)
                   context)))
    ->interceptor))

;; -- Handlers --------------------------------------------------------------

(reg-event-db
  :initialize-db
  ;;[validate-spec]
  (fn [_ _]
    app-db))

(reg-event-db
  :set-greeting
  ;;[validate-spec]
  (fn [db [_ value]]
    (assoc db :greeting value)))

(reg-event-db
 :set-messages
 ;;[validate-spec]
 (fn [db [_ value]]
   (assoc db :messages value)))


;; Also add new message to Firebase
(reg-event-db
 :send-message
 ;;[validate-spec]
 (fn [db [_ msg]]
   (let [username (db :greeting)]
     (fb-add-msg username msg)
     db 
     )))


;; Receive a new message from firebase - add it to the end of :messages in local db
(reg-event-db
 :receive-message
 ;;[validate-spec]
 (fn [db [_ {:keys [username text] :as msg}]]
   (print ":receive-message " msg)
   (assoc db :messages (conj (db :messages) {:user username :text text}))))
