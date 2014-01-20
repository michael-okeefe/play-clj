(in-ns 'play-clj.core)

; graphics

(defn clear!
  "Clears the screen with a uniform color, defaulting to black

    (clear!)
    (clear! 0.5 0.5 1 1)
"
  ([]
    (clear! 0 0 0 0))
  ([r g b a]
    (doto (Gdx/gl)
      (.glClearColor (float r) (float g) (float b) (float a))
      (.glClear GL20/GL_COLOR_BUFFER_BIT))))

(defmacro color
  "Returns a [Color](http://libgdx.badlogicgames.com/nightlies/docs/api/com/badlogic/gdx/graphics/Color.html)
object"
  [& args]
  `~(if (keyword? (first args))
      `(Color. ^Color (u/static-field-upper :graphics :Color ~(first args)))
      `(Color. ~@args)))

; interop

(defmacro app!
  "Calls a single method on [Gdx.app](http://libgdx.badlogicgames.com/nightlies/docs/api/com/badlogic/gdx/Application.html)

    (app! :error \"MYTAG\" \"An error occurred, so I'm logging it!\")
"
  [k & options]
  `(u/call! ^Application (Gdx/app) ~k ~@options))

(defmacro audio!
  "Calls a single method on [Gdx.audio](http://libgdx.badlogicgames.com/nightlies/docs/api/com/badlogic/gdx/Audio.html)

    (audio! :new-audio-recorder 44100 false)
"
  [k & options]
  `(u/call! ^Audio (Gdx/audio) ~k ~@options))

(defmacro files!
  "Calls a single method on [Gdx.files](http://libgdx.badlogicgames.com/nightlies/docs/api/com/badlogic/gdx/Files.html)

    (files! :internal \"image.png\")
"
  [k & options]
  `(u/call! ^Files (Gdx/files) ~k ~@options))

(defmacro gl!
  "Calls a single method on [Gdx.gl20](http://libgdx.badlogicgames.com/nightlies/docs/api/com/badlogic/gdx/graphics/GL20.html)

    (gl! :gl-create-program)
"
  [k & options]
  `(u/call! ^GL20 (Gdx/gl20) ~k ~@options))

(defmacro graphics!
  "Calls a single method on [Gdx.graphics](http://libgdx.badlogicgames.com/nightlies/docs/api/com/badlogic/gdx/Graphics.html)

    (graphics! :is-fullscreen)
"
  [k & options]
  `(u/call! ^Graphics (Gdx/graphics) ~k ~@options))

(defmacro input!
  "Calls a single method on [Gdx.input](http://libgdx.badlogicgames.com/nightlies/docs/api/com/badlogic/gdx/Input.html)

    (input! :is-touched)
"
  [k & options]
  `(u/call! ^Input (Gdx/input) ~k ~@options))

(defmacro net!
  "Calls a single method on [Gdx.net](http://libgdx.badlogicgames.com/nightlies/docs/api/com/badlogic/gdx/Net.html)

    (net! :open-uri \"https://nightcode.info/\")
"
  [k & options]
  `(u/call! ^Net (Gdx/net) ~k ~@options))

; input/output

(defn game
  [k]
  (case k
    :width (graphics! :get-width)
    :height (graphics! :get-height)
    :fps (graphics! :get-frames-per-second)
    :is-fullscreen? (graphics! :is-fullscreen)
    :is-touched? (input! :is-touched)
    :x (input! :get-x)
    :y (input! :get-y)
    (u/throw-key-not-found k)))

(defmacro key-code
  [k]
  `~(symbol (str u/main-package ".Input$Keys/" (u/key->upper k))))

(defmacro is-pressed?
  [k]
  `(input! :is-key-pressed (key-code ~k)))

(defn ^:private add-input!
  [^InputProcessor p]
  (let [^InputMultiplexer multi (input! :get-input-processor)]
    (.addProcessor multi p)))

(defn ^:private remove-input!
  [^InputProcessor p]
  (let [^InputMultiplexer multi (input! :get-input-processor)]
    (.removeProcessor multi p)))
