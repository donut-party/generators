(ns {{ns}}
  (:require
   [donut.frontend.form.components :as dfc]
   [donut.frontend.form.flow :as dff]))

(defn component
  []
  [:div {:class "prose"}
   [:h1 "{{entity-name-plural}}"]
   (dfc/with-form {::dff/form-key :new-{{entity-name-singular}}}
     [:div
      [*input {:type                  :text
               :class                 ["input"]
               :placeholder           "name"
               :donut.input/attr-path :name}]])])
