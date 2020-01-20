(ns grpmsg.core
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [oops.core :refer [ocall]]
           ;; [cljs.core.async :as async]
           ;; [firemore.core :as firemore]
            [grpmsg.handlers]
            [grpmsg.subs])
  ;;(:require-macros
  ;; [cljs.core.async.macros :refer [go-loop go]])
)

; Import Firebase SDK
(defonce firebase  (js/require "firebase/app"))
(js/require "firebase/firestore")

; Firebase configuration for this app
(defonce firebaseConfig #js {
    :apiKey "AIzaSyCTl1UP8wxK7rSMvR8Kx8NhysmLcXDoewY",
    :authDomain "groupmessage-2c107.firebaseapp.com",
    :databaseURL "https://groupmessage-2c107.firebaseio.com",
    :projectId "groupmessage-2c107",
    :storageBucket "groupmessage-2c107.appspot.com",
    :messagingSenderId "158692618500",
    :appId "1:158692618500:web:aea01ac266f59c7d9d2051"
  })

; Initialize Firebase
(defonce not-used (.initializeApp firebase firebaseConfig))
(defonce firestore (.dbStore firebase))

(def ReactNative (js/require "react-native"))
(def expo (js/require "expo"))
(def AtExpo (js/require "@expo/vector-icons"))
(def ionicons (.-Ionicons AtExpo))
(def ic (r/adapt-react-class ionicons))

(def text (r/adapt-react-class (.-Text ReactNative)))
(def text-input (r/adapt-react-class (.-TextInput ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def flat-list (r/adapt-react-class (.-FlatList ReactNative)))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def Alert (.-Alert ReactNative))

(defn alert [title]
  (.alert Alert title))

(deftype Item [value]
  IEncodeJS
  (-clj->js [x] (.-value x))
  (-key->js [x] (.-value x))
  IEncodeClojure
  (-js->clj [x _] (.-value x)))


(defn to-js-array
  "Converts a collection to a JS array (but leave content as is)"
  [coll]
  (let [arr (array)]
    (doseq [x coll]
      (.push arr x))
    arr))

(defn wrap-data [o]
  (Item. (to-js-array o)))

;;Flat-list item render function
(defn render-item [item index separators]
  [view {:style {:flex-direction "column"}}
   [text (str (:name item) ": ") ]
   [text (:message item)]
   [text " "]]
  )

(defn app-root []
  (let [chat (subscribe [:get-messages])
        my-name (r/atom "")
        message (r/atom "")]
    (fn []
      [view {:style {:flex-direction "column" :margin 20 :align-items "stretch" }}
       [view {:style {:flex-direction "row"  :justify-content "space-between"}}
        [text {:style {:padding 10}} "I am:"]
        [text-input {:style {:borderColor "gray" :borderWidth 1 :width 200}
                     :onChangeText  #(reset! my-name %)
                     :value @my-name}]

        [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5}
                              :on-press #(alert (str "Connect as  " @my-name))}
         [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "connect"]]]

       [flat-list {:style {:borderColor "gray" :borderWidth 1 :padding 10 :height 500}
                   :data (wrap-data [{:name "John" :message "Hello Mike" :id "id1"}
                                     {:name "Mike" :message "Hi John" :id "ida"}])
                   :key-extractor (fn [item index] (str index)) ;;(get-in item [:message :id])))
                   :renderItem (fn [data]
                                 (r/as-element (render-item (.-item data) (.-index data) (.-separators data))))}]


       [view {:style { :flex-direction "row"}}
        [text-input {:style {:borderColor "gray" :borderWidth 1 :width 300 :padding 10}
                     :onChangeText  #(reset! message %)
                     :onSubmitEditing #(alert @message)
                     :value @message}]
        [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5}
                              :on-press #(alert (str "Send: " @message))}
         [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "send"]]]])))


(defn init []
  (dispatch-sync [:initialize-db])
  (ocall expo "registerRootComponent" (r/reactify-component app-root)))
