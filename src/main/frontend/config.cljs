(ns frontend.config
  "App config and fns built on top of configuration"
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [frontend.state :as state]
            [frontend.util :as util]
            [goog.crypt.Md5]
            [logseq.common.config :as common-config]
            [logseq.common.path :as path]
            [logseq.db.sqlite.util :as sqlite-util]
            [shadow.resource :as rc]))

(goog-define DEV-RELEASE false)
(defonce dev-release? DEV-RELEASE)
(defonce dev? ^boolean (or dev-release? goog.DEBUG))

(defonce publishing? common-config/PUBLISHING)

(goog-define REVISION "unknown")
(defonce revision REVISION)

(goog-define ENABLE-FILE-SYNC-PRODUCTION false)

;; this is a feature flag to enable the account tab
;; when it launches (when pro plan launches) it should be removed
(def ENABLE-SETTINGS-ACCOUNT-TAB false)

(do (def LOGIN-URL
      "https://ap-southeast-2ngw6pg4gc.auth.ap-southeast-2.amazoncognito.com/login?client_id=7gk3dcuo5klcn848v449llu2p8&response_type=code&scope=email+openid+phone&redirect_uri=logseq%3A%2F%2Fauth-callback")
    (def API-DOMAIN "api-dev.logseq.com")
    (def COGNITO-IDP "https://cognito-idp.ap-southeast-2.amazonaws.com/")
    (def COGNITO-CLIENT-ID "7gk3dcuo5klcn848v449llu2p8")
    (def REGION "ap-southeast-2")
    (def USER-POOL-ID "ap-southeast-2_NGw6Pg4GC")
    (def IDENTITY-POOL-ID "ap-southeast-2:588029ea-27f2-4ab2-aac9-ac6c1b855a17")
    (def OAUTH-DOMAIN "ap-southeast-2ngw6pg4gc.auth.ap-southeast-2.amazoncognito.com")
    (def PUBLISH-API-BASE "https://logseq-publish-staging.logseq.workers.dev"))

;; Enable for local development
;; (def PUBLISH-API-BASE "http://localhost:8787")

(goog-define ENABLE-DB-SYNC-LOCAL false)
(defonce db-sync-local? ENABLE-DB-SYNC-LOCAL)

(defonce db-sync-ws-url
  (if db-sync-local?
    "ws://127.0.0.1:8787/sync/%s"
    "wss://api.logseq.io/sync/%s"))

(defonce db-sync-http-base
  (if db-sync-local?
    "http://127.0.0.1:8787"
    "https://api.logseq.io"))

;; Feature flags
;; =============

(goog-define ENABLE-PLUGINS true)
(defonce feature-plugin-system-on? ENABLE-PLUGINS)

;; Desktop only as other platforms requires better understanding of their
;; multi-graph workflows and optimal place for a "global" dir
(def global-config-enabled? util/electron?)

;; User level configuration for whether plugins are enabled
(defonce lsp-enabled?
  (and util/plugin-platform?
       (not (false? feature-plugin-system-on?))
       (state/lsp-enabled?-or-theme)))

(defn plugin-config-enabled?
  []
  (and lsp-enabled? (global-config-enabled?)))

;; :TODO: How to do this?
;; (defonce desktop? ^boolean goog.DESKTOP)

;; ============

(def app-name common-config/app-name)

;; FIXME:
(def app-website
  (if dev?
    "http://localhost:3001"
    (util/format "https://%s.com" app-name)))

(def markup-formats
  #{:org :md :markdown :asciidoc :adoc :rst})

(def doc-formats
  #{:doc :docx :xls :xlsx :ppt :pptx :one :pdf :epub})

(def image-formats
  #{:png :jpg :jpeg :bmp :gif :webp :svg :heic})

(def audio-formats
  #{:mp3 :ogg :mpeg :wav :m4a :flac :wma :aac})

(def video-formats
  #{:mp4 :webm :mov :flv :avi :mkv})

(def media-formats (set/union (common-config/img-formats) audio-formats video-formats))

(def mobile?
  "Triggering condition: Mobile phones
   *** Warning!!! ***
   For UX logic only! Don't use for FS logic
   iPad / Android Pad doesn't trigger!

   Same as config/mobile?"
  (when-not util/node-test?
    (util/safe-re-find #"Mobi" js/navigator.userAgent)))

(defn get-hr
  [format]
  (let [format (or format (keyword (state/get-preferred-format)))]
    (case format
      :markdown
      "---"
      "")))

(defn get-bold
  [format]
  (let [format (or format (keyword (state/get-preferred-format)))]
    (case format
      :markdown
      "**"
      "")))

(defn get-italic
  [format]
  (let [format (or format (keyword (state/get-preferred-format)))]
    (case format
      :markdown
      "*"
      "")))
(defn get-underline
  [format]
  (let [format (or format (keyword (state/get-preferred-format)))]
    (case format
      :markdown ;; no underline for markdown
      ""
      "")))
(defn get-strike-through
  [format]
  (let [format (or format (keyword (state/get-preferred-format)))]
    (case format
      :markdown
      "~~"
      "")))

(defn get-highlight
  [format]
  (case format
    :markdown
    "=="
    ""))

(defn get-code
  [format]
  (let [format (or format (keyword (state/get-preferred-format)))]
    (case format
      :markdown
      "`"
      "")))

(defn get-empty-link-and-forward-pos
  [format]
  (case format
    :markdown
    ["[]()" 1]
    ["" 0]))

(defn link-format
  [label link]
  (if (not-empty label)
    (util/format "[%s](%s)" label link)
    link))

(defn with-default-link
  [format link]
  (case format
    :markdown
    [(util/format "[](%s)" link)
     1]
    ["" 0]))

(defn with-label-link
  [format label link]
  (case format
    :markdown
    [(util/format "[%s](%s)" label link)
     (+ 4 (count link) (count label))]
    ["" 0]))

(defn with-default-label
  [format label]
  (case format
    :markdown
    [(util/format "[%s]()" label)
     (+ 3 (count label))]
    ["" 0]))

(defonce demo-repo "Demo")

(defn demo-graph?
  "Demo graph or nil graph?"
  ([]
   (demo-graph? (state/get-current-repo)))
  ([repo-url]
   (or (nil? repo-url) (= repo-url demo-repo)
       (string/ends-with? repo-url demo-repo))))

(def config-file "config.edn")
(def custom-css-file "custom.css")
(def export-css-file "export.css")
(def custom-js-file "custom.js")
(def config-default-content (rc/inline "templates/config.edn"))

;; NOTE: repo-url is the unique identifier of a repo.
;; - `logseq_db_GraphName` => db based graph, sqlite as backend
;; - Use `""` while writing global files

(defonce db-version-prefix common-config/db-version-prefix)

(defn db-graph-name
  [repo-with-prefix]
  (string/replace-first repo-with-prefix db-version-prefix ""))

(defn db-based-graph?
  ([]
   (db-based-graph? (state/get-current-repo)))
  ([s]
   (boolean
    (and (string? s)
         (sqlite-util/db-based-graph? s)))))

(defn get-local-asset-absolute-path
  [s]
  (str "/" (string/replace s #"^[./]*" "")))

(defn get-local-dir
  [repo]
  (path/path-join (get-in @state/state [:system/info :home-dir])
                  "logseq"
                  "graphs"
                  (string/replace repo db-version-prefix "")))

(defn get-electron-backup-dir
  [repo]
  (path/path-join (get-local-dir repo) "backups"))

(defn get-repo-dir
  [repo-url]
  (when repo-url
    (if (util/electron?)
      (get-local-dir repo-url)
      (str "memory:///"
           (string/replace-first repo-url db-version-prefix "")))))

(defn get-repo-config-path
  []
  (path/path-join app-name config-file))

(defn get-custom-css-path
  ([]
   (get-custom-css-path (state/get-current-repo)))
  ([repo]
   (if (db-based-graph? repo)
     (path/path-join app-name custom-css-file)
     (when-let [repo-dir (get-repo-dir repo)]
       (path/path-join repo-dir app-name custom-css-file)))))

(defn get-export-css-path
  ([]
   (get-export-css-path (state/get-current-repo)))
  ([repo]
   (when-let [repo-dir (get-repo-dir repo)]
     (path/path-join repo-dir app-name  export-css-file))))

(defn get-current-repo-assets-root
  []
  (when-let [repo-dir (get-repo-dir (state/get-current-repo))]
    (path/path-join repo-dir "assets")))

(defn get-repo-assets-root
  [repo]
  (when-let [repo-dir (get-repo-dir repo)]
    (path/path-join repo-dir "assets")))

(defn get-custom-js-path
  ([]
   (get-custom-js-path (state/get-current-repo)))
  ([repo]
   (if (db-based-graph? repo)
     (path/path-join app-name custom-js-file)
     (when-let [repo-dir (get-repo-dir repo)]
       (path/path-join repo-dir app-name custom-js-file)))))
