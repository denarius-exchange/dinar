(ns dinar.handler
  (:require [compojure.core :refer [defroutes GET]]
            [dinar.routes.home :refer [home-routes]]
            [dinar.middleware :as middleware]
            [noir.util.middleware :refer [app-handler]]
            [noir.util.route :refer [restricted]]
            [noir.session :as session]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.rotor :as rotor]
            [selmer.parser :as parser]
            [environ.core :refer [env]]
            [dinar.layout :as layout]
            [dinar.routes.auth :refer [auth-routes]]
            [dinar.db.schema :as schema]
            [dinar.routes.cljsexample :refer [cljs-routes]]))



(defn main-page []
  (layout/render "main.html"))

(defroutes
  app-routes
  (GET "/main" [] (restricted (main-page)))
  (route/resources "/")
  (route/not-found "Not Found"))

(defn init
  "init will be called once when\r
   app is deployed as a servlet on\r
   an app server such as Tomcat\r
   put any initialization code here"
  []
  (timbre/set-config!
    [:appenders :rotor]
    {:min-level :info,
     :enabled? true,
     :async? false,
     :max-message-per-msecs nil,
     :fn rotor/appender-fn})
  (timbre/set-config!
    [:shared-appender-config :rotor]
    {:path "dinar.log", :max-size (* 512 1024), :backlog 10})
  (if (env :dev) (parser/cache-off!))
  (if-not (schema/initialized?) (schema/create-tables))
  (timbre/info "dinar started successfully"))

(defn destroy
  "destroy will be called when your application\r
   shuts down, put any clean up code here"
  []
  (timbre/info "dinar is shutting down..."))

(defn user-access [request]
  (session/get :user-id))

(def app
 (app-handler
   [cljs-routes auth-routes home-routes app-routes]
   :middleware [middleware/template-error-page middleware/log-request]
   :access-rules [user-access]
   :formats [:json-kw :edn]))

