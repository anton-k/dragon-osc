{-# Language OverloadedStrings #-}
module Dragon.Osc(
    Size, Float2, Int2,
    Root(..), Window(..), Ui(..), Param(..), 
    Keys, KeyEvent(..), 
    HotKey(..), withModifiers, ctrl, shift, meta, alt,
    Send(..), Msg(..), Args, Arg(..), 
    Page(..), Sym(..), Orient(..),
    setId, setSend, setMsgs, setMsg, ui, sendMsg, onBool, onBools, 
    multiUi,
    writeJson
) where

import Data.Aeson
import Data.String
import Control.Arrow(first, second)
import qualified Data.Map as M
import Data.Monoid
import Data.Maybe

import qualified Data.ByteString.Lazy as LB

type Size = (Int, Int)
type Float2 = (Float, Float)
type Int2 = (Int, Int)

data Root = Root 
    { rootWindows :: [Window] 
    , rootKeys    :: Keys 
    , rootInitOsc :: [Msg] }

data Window = Window 
    { windowTitle   :: String
    , windowSize    :: Maybe Size    
    , windowContent :: Ui
    , windowKeys    :: Keys }

data Ui = Ui 
    { uiSym     :: Sym
    , uiParam   :: Param  }

data Param = Param 
    { paramId   :: Maybe String
    , paramSend :: Maybe Send }

type Keys = [KeyEvent]
data KeyEvent = KeyEvent { key:: HotKey, send:: Send }
newtype HotKey = HotKey { unHotKey :: [String] }

instance IsString HotKey where
    fromString key = HotKey [key]

withModifiers :: [String] -> HotKey -> HotKey
withModifiers xs (HotKey ys) = HotKey (ys ++ xs)

ctrl, shift, meta, alt :: HotKey -> HotKey

ctrl  = withModifiers ["ctrl"]
shift = withModifiers ["shift"]
meta  = withModifiers ["meta"]
alt   = withModifiers ["alt"]

data Send = Send 
    { sendDefault :: [Msg]
    , onValue :: [(String, [Msg])]
    , onValueOff :: [(String, [Msg])] }

data Msg = Msg 
        { msgClient :: String
        , msgPath   :: String
        , msgArgs   :: Args }
    | DelayedMsg
        { msgClient :: String
        , msgPath   :: String
        , msgArgs   :: Args
        , msgDelay  :: Float }

type Args = [Arg]

data Arg = ArgString String | ArgFloat Float | ArgBool Bool | ArgInt Int | Arg Int | Mem String

data Page = Page 
    { pageTitle :: String
    , pageContent:: Ui
    , pageKeys :: Keys }

data Sym 
    = Hor [Ui]
    | Ver [Ui]

    | Tabs [Page]
    | Space
    | Glue

    | Dial   { init:: Float, color:: String, range:: Float2 }
    | HFader { init:: Float, color:: String, range:: Float2 }
    | VFader { init:: Float, color:: String, range:: Float2 }

    | Toggle { initBool  :: Bool, color:: String, text:: String }
    | IntDial { initBool :: Bool, color:: String, rangeInt :: Int2 }
    | Button { color:: String, text:: String }
    | Label  { color:: String, text:: String }

    | CircleToggle { initBool :: Bool, color :: String }
    | CircleButton { color :: String }

    | MultiToggle { initSet :: [Int2], size:: Size, color:: String, texts:: [String] }
    | HCheck      { initInt :: Int,   leng:: Int, color:: String, texts:: [String], allowDeselect :: Maybe Bool }
    | VCheck      { initInt :: Int,   leng:: Int, color:: String, texts:: [String], allowDeselect :: Maybe Bool }

    | XYPad { initFlow2:: Float2, color:: String }
    | HFaderRange { initFlow2:: Float2, color:: String }
    | VFaderRange { initFlow2:: Float2, color:: String }
    | XYPadRange { initX:: Float2, initY:: Float2, color:: String }

    | DropDownList { initInt:: Int, texts:: [String] }
    | TextInput { initString:: Maybe String, color:: String, textLength :: Maybe Int }    
    | FileInput { initString:: Maybe String, color:: String, text:: String }
    | DoubleCheck { initInt2:: Int2, sizes:: [Int], color1:: String, color2:: String, doubleTexts:: [(String, [String])], orient:: Orient, allowDeselect:: Maybe Bool }

data Orient = Orient { orientIsFirst :: Bool, orientIsFirstHor :: Bool, orientIsSecondHor :: Bool }

-------------------------------------------------------------
-- aux

setId :: String -> Ui -> Ui
setId name (Ui sym param) = Ui sym (param { paramId = Just name })

setSend :: Send -> Ui -> Ui
setSend send (Ui sym param) = Ui sym (param { paramSend = Just send })

setMsgs :: [Msg] -> Ui -> Ui
setMsgs = setSend . (\msgs -> Send msgs [] [])

setMsg :: Msg -> Ui -> Ui
setMsg = setMsgs . return

ui x = Ui x (Param Nothing Nothing)

sendMsg :: Msg -> Send
sendMsg msg = Send [msg] [] []

onBool :: Msg -> Msg ->  Ui -> Ui
onBool on off = onBools [on] [off]

onBools :: [Msg] -> [Msg] ->  Ui -> Ui
onBools ons offs = setSend (Send [] onVal []) 
    where onVal = [("true", ons), ("false", offs)]

-------------------

multiUi :: (Int, Int) -> (Int -> Ui) -> Ui
multiUi (xSize, ySize) unit = ui $ Ver $ fmap row $ take ySize [0, xSize .. ]
    where
        row n = ui $ Hor $ fmap (unit . (+ n)) [0 .. xSize - 1]

-------------------------------------------------------------
-- toJson

instance ToJSON Root where
    toJSON (Root windows keys initSend) = object ["main" .= windows, "keys" .= keys, "init-send" .= initSend]

instance ToJSON Window where
    toJSON (Window title size content keys) = "window" =: (object $ catMaybes [Just $ "title" .= title, fmap (\sz -> "size" .=  [fst sz, snd sz]) size, Just $ "content" .= content, Just $ "keys" .= keys])

instance ToJSON Orient where
    toJSON (Orient isFirst isFirstHor isSecondHor) = toJSON $ [isFirst, isFirstHor, isSecondHor]

instance ToJSON Ui where
    toJSON (Ui sym param) = case toJSON sym of
        Object obj -> case toJSON param of
            Object paramObj -> Object (obj <> paramObj)
            _               -> Object obj
        value -> value

instance ToJSON Param where
    toJSON (Param id send) = object $ catMaybes [fmap ("id" .= ) id, fmap ("send" .= ) send]

(=:) :: (ToJSON a) => String -> a -> Value
(=:) name value = toJSON $ M.fromList [(name, value)]

instance ToJSON Page where
    toJSON p = "page" =: object ["title" .= pageTitle p, "content" .= pageContent p, "keys" .= pageKeys p ] 

instance ToJSON KeyEvent where
    toJSON k = object ["key" .= key k, "send" .=  send k ]
        where 
            

instance ToJSON HotKey where
    toJSON (HotKey xs) = case xs of
        [k] -> toJSON k
        _   -> toJSON xs

instance ToJSON Send where
    toJSON (Send defaults onValue onValueOff) = object $ ("default" .= defaults) : (tfm "case" onValue ++ tfm "case-off" onValueOff)
            where tfm name m = fmap (uncurry (.=)) $ fmap (first (fromString . ( (name ++ " ") ++ ))) m

instance ToJSON Msg where
    toJSON (Msg client path args) = "msg" =: object ["client" .= client, "path" .= path, "args" .= args]

    toJSON (DelayedMsg client path args delay) = "msg" =: object ["client" .= client, "path" .= path, "args" .= args, "delay" .= delay]


instance ToJSON Arg where
    toJSON a = case a of
        ArgString x -> toJSON x
        ArgFloat x  -> toJSON x 
        ArgBool x -> toJSON x 
        ArgInt x -> toJSON x
        Arg n -> toJSON $ '$' : show n
        Mem name -> toJSON $ '$' : name

instance ToJSON Sym where
    toJSON x = case x of
        Hor xs -> "hor" =: xs
        Ver xs -> "ver" =: xs

        Tabs ps -> "tabs" =: ps
        Space   -> "space" =: object []
        Glue    -> "glue"  =: object []

        Dial init color range   -> floatVal "dial"   init color range
        HFader init color range -> floatVal "hfader" init color range
        VFader init color range -> floatVal "vfader" init color range

        Toggle init color text -> "toggle" =: object [ "init" .= init, "color" .= color, "text" .= text ]
        IntDial init color range -> "int-dial" =: object [ "init" .= init, "color" .= color, "range" .= fromPair range ]
        Button color text -> "button" =: object [ "color" .= color, "text" .= text ]
        Label color text -> "label" =: object [ "color" .= color, "text" .= text ]

        CircleToggle init color -> "circle-toggle" =: object [ "init" .= init, "color" .= color ]
        CircleButton color -> "circle-button" =: object [ "color" .= color ]

        MultiToggle  initSet size color texts -> "multi-toggle" =: object [ "init" .= initSet, "color" .= color, "texts" .= texts, "size" .= fromPair size ]
        HCheck init leng color texts allowDeselect -> "hcheck" =: (object $ (let xs = [ "init" .= init, "size" .= leng, "color" .= color, "texts" .= texts ] in maybe xs (:xs) (fmap ("allow-deselect" .=) allowDeselect)))
        VCheck init leng color texts allowDeselect -> "vcheck" =: (object $ (let xs = [ "init" .= init, "size" .= leng, "color" .= color, "texts" .= texts ] in maybe xs (:xs) (fmap ("allow-deselect" .=) allowDeselect)))

        XYPad (initX, initY) color -> "xy-pad" =: object [ "init" .= [initX, initY], "color" .= color ]

        DropDownList init texts -> "drop-down-list" =: object [ "init" .= init, "texts" .= texts ]
        TextInput maybeInit color textLength -> "text-input" =: (object $ catMaybes [fmap ("init" .= ) maybeInit, Just $ "color" .= color, fmap ("text-length" .= ) textLength])
        FileInput maybeInit color text -> "file-input" =: (object $ catMaybes [fmap ("init" .= ) maybeInit, Just $ "color" .= color, Just $ "text" .= text  ])    
        DoubleCheck initInt2 sizes color1 color2 doubleTexts orient allowDeselect -> "double-check" =: object [ "init" .= [fst initInt2, snd initInt2], "sizes" .= sizes, "color1" .= color1, "color2" .= color2, "texts" .= doubleTexts, "orient" .= orient ]
        where 
            floatVal name init color (rangeMin, rangeMax) = name =: object [ "init" .= init, "color" .= color, "range" .= [rangeMin, rangeMax] ]
            fromPair (a, b) = [a, b]

-----------------------------------------------------------------

writeJson :: String -> Root -> IO ()
writeJson filename root = do
    LB.writeFile "test.json" $ encode root
