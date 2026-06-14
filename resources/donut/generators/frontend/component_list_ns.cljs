(ns {{ns}}
  (:require
   [donut.compose :as dc]
   [donut.frontend.core.utils :as dcu]
   [donut.frontend.events :as dfe]
   [donut.frontend.form.components :as dfc]
   [donut.frontend.form.flow :as dff]
   [donut.frontend.sync.flow :as dsf]
   [re-frame.core :as rf]))

;; TODO move this to the subs namespace
(rf/reg-sub {{entity-name-plural-kw}}
  (fn [db]
    (dcu/entities db {{entity-name-singular-kw}})))

(defn {{entity-name-plural}}
  []
  [:div
   (->> @(rf/subscribe [{{entity-name-plural-kw}}])
        (mapv (fn [x] ^{:key ({{id-key}} x)} [:li ({{entity-name-plural-kw}}/name x)]))
        (into [:ul]))])

(defn component
  []
  [:div {:class "prose"}
   [:h1 "{{entity-name-plural}}"]
   (dfc/with-form {::dff/form-key :new-{{entity-name-singular}}}
     [:form {:on-submit
             (dcu/prevent-default
                     (fn [_]
                       (*sync-form {::dsf/req {:method     :post
                                               :route-name {{collection-route-name}}}
                                    ::dfe/on  {:success (dc/into [[::dff/clear-form]])}})))}
      [:div
       [*input {:type                  :text
                :class                 ["input"]
                :placeholder           "name"
                :donut.input/attr-path {{entity-name-plural-kw}}/name}]]])
   [{{entity-name-plural}}]])
