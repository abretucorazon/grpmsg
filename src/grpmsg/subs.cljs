(ns grpmsg.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :get-greeting
 (fn [db _]
   (:greeting db)))

(reg-sub
 :get-messages
 (fn [db _]
   (:messages db)))