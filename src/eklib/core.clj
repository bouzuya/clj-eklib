(ns eklib.core
  (:use [clojure.pprint])
  (:import (java.io StringReader))
  (:require [net.cgrand.enlive-html :as eh]
            [clj-http.cookies :as cookies]
            [clj-http.client :as client]))

(defn get-signin-url [cookie-store]
  (let [response (client/get "http://www.amazon.co.jp/" {:as "Shift_JIS" :cookie-store cookie-store})]
    (let [html (eh/html-resource (java.io.StringReader. (:body response)))]
      (let [es (eh/select html [:span.navMessage :a])]
         (first (eh/attr-values (first es) :href))))))

(defn signin [cookie-store mailaddr password]
  (let [signin-url (get-signin-url cookie-store)
        response (client/get signin-url {:as "Shift_JIS" :cookie-store cookie-store})
        html (eh/html-resource (java.io.StringReader. (:body response)))
        url (first (eh/attr-values (first (eh/select html [:form#ap_signin_form])) :action))
        params (reduce
                 (fn [m e]
                   (assoc m (keyword (first (eh/attr-values e :name)))
                            (first (eh/attr-values e :value))))
                 {}
                 (eh/select html #{[(eh/attr-has :type "hidden")]
                                   [(eh/attr-has :type "text")]}))]
          (client/post
            url
            {:form-params (-> params
                              (assoc :email mailaddr)
                              (assoc :password password))
             :cookie-store cookie-store})))

(defn get-recommends [cookie-store]
  (let [response (client/get
                   "http://www.amazon.co.jp/gp/yourstore/recs/"
                   {:as "Shift_JIS" :cookie-store cookie-store})
        html (eh/html-resource (java.io.StringReader. (:body response)))
        div (eh/select html [:div.ys :table :td :a :strong])]
    (map #(apply str (eh/unwrap %)) div)))

(defn -main [& args]
  (let [cookie-store (cookies/cookie-store)
        mailaddr (first args)
        password (second args)]
    (do
      (signin cookie-store mailaddr password)
      (pprint (get-recommends cookie-store)))))

