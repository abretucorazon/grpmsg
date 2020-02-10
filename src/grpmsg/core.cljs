(ns grpmsg.core
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [oops.core :refer [ocall]]
            [grpmsg.firebase-config :refer [firebase-config]]
            [grpmsg.handlers]
            [grpmsg.subs])
)


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
   [text (str (:user item) ": " (:text item)) ]
   [text " "]]
  )

(defn app-root []
  (let [chat (subscribe [:get-messages])
        my-name (r/atom @(subscribe [:get-greeting] ))
        message (r/atom "")]
    (fn []
      [view {:style {:flex-direction "column" :margin 20 :align-items "stretch"}}
       [view {:style {:flex-direction "row"  :justify-content "space-between"}}
        [text {:style {:padding 10}} "I am:"]
        [text-input {:style {:borderColor "gray" :borderWidth 1 :width 200}
                     :onChangeText  #(reset! my-name %)
                     :value @my-name}]

        [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5}
                              :on-press #(dispatch [:set-greeting @my-name])}
        [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "connect"]]]

       [flat-list {:style {:borderColor "gray" :borderWidth 1 :padding 10 :height 500}
                   :data (wrap-data @chat)
                   :keyExtractor (fn [data index] (str index));;(.-item data))
                   :renderItem (fn [data]
                                 (r/as-element (render-item (.-item data) (.-index data) (.-separators data))))}]
                   
       [view {:style {:flex-direction "row"}}
        [text-input {:style {:borderColor "gray" :borderWidth 1 :width 300 :padding 10}
                     :onChangeText  #(reset! message %)
                     :value @message}]
        [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5}
                              :on-press (fn []
                                          (dispatch [:send-message @message])
                                          (reset! message ""))}
         [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "send"]]]])))


(defn init []
  (dispatch-sync [:initialize-db])
  (ocall expo "registerRootComponent" (r/reactify-component app-root)))
