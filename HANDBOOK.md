# The Handbook for extension creation on the Echo media player  
Thanks for choosing to contribute to Echo by creating your own extension, we will aim to facilitate the process of creating the extension, unlike Shivam who expects you to read the source code for Echo to figure out how to write an extension, this easy-to-understand guide will give you the necessary details and steps to contribute with the less effort spent  
> [!NOTE]  
> You are still expected to have some knowledge in coding (kotlin) and creating Android projects  

## Coding practices  
It is adviced to use async code (suspended) for functions that send out a web request and return (await) the response  
It is adviced to load the classes you need in the main class file lazily (`val api by lazy { someApiClass() }`) in which you can pass settings, but please note that lazy loading might cause some issues (for example you can instanciate a Settings object inside a lazy loaded class, you'll need to pass a loaded settings object instead)  
It is advised to use modularization wherever possible to help readabily and shareability  

## Setting up your building system  
**For people who use Android Studio please skip this part**  
Create a keystore by issuing the following command:  
```sh
keytool -genkeypair -v -keystore my-release-key.keystore -alias my-app-key -keyalg RSA -keysize 2048 -validity 10000
```
Make sure to save this file somewhere safe OUTSIDE THE REPO, I usually place it in `~/.keystore/`  
Take note of the __key alias__ and the __password__  
Save your keystore password to a file, most effectively done like so:  
```sh
echo "YOUR KEYSTORE PASSWORD" > pass.txt
```
And make sure to place it OUTSIDE THE REPO as well  
Create a build script that contains the following:  
```sh
#!/bin/bash

pass=$(cat "/home/nekomimi/.keystore/pass.txt")
./gradlew assembleRelease \
    -Pandroid.injected.signing.store.file="/home/nekomimi/.keystore/my-release-key.keystore" \
    -Pandroid.injected.signing.store.password="$pass" \
    -Pandroid.injected.signing.key.alias="my-app-key" \
    -Pandroid.injected.signing.key.password="$pass"
```
Make sure to replace the actual location of the keystore file and password file to your own location  
To disable debugging features when building:  
- Open `app/build.gradle.kts`  
- Inside the `Android{}` scope, find `buildTypes`, generally you'll only find an `all` entry, inside it (`all`) add `isMinifyEnabled = true` inside the scope  

## What is an extension ?  
We can't dive straight into creating the extension without first knowing how one works, apart from the main code that sets up a kotlin/Android project you start with your main class, that class extends multiple interfaces to add the features you need (lets say you want to create an extension that only does lyrics, then you'd need the LyricsClient interface)  
We will divide our code into segements to keep things easier to read and help others be able to contrib to your project, we will also stick to the kotlin indent style and syntax rules (because people will shit on you otherwise)  
Your main class file should only contain declarations and overrides, you are advised to not implement any of the logic in this file but rather to call functions from other files  
> [!NOTE]  
> We do not have control over your style of code but please trust me on this and follow the rules I've set  

## Main Interfaces  
Your main class extends some interfaces to define what it supports  
These interfaces also require you to add/override some functions to do the logic that they provide  
| Interface | Overrides | Description |
| --- | --- | --- |
| `ExtensionClient` | `onExtensionSelected()` `settingItems` `setSettings` | The main interface needed by all extensions to initialize the extension and provide settings |
| `HomeFeedClient` | `getHomeTabs()` `getHomeFeed()` | To set the home feed provider (The Home tab at the bottom nav) |
| `SearchFeedClient` | `searchHistory` `saveQueryToHistory()` `quickSearch()` `deleteQuickSearch()` `searchTabs()` `searchFeed()` | All related search features including history handling and the search feed (The Search tab at the bottom nav) |
| `TrackClient` | `loadTrack()` `loadStreamableMedia()` `getShelves()` | This is where the track processing happens, needed for extensions that provide tracks to play |  

These are probably the options you need, we will leave the rest in a different table since they are less used and the ones above will count as the essentials  
| Interface | Overrides | Description |
| --- | --- | --- |
| `LyricsClient` | `searchTrackLyrics()` `loadLyrics()` | Needed to setup a lyrics provider or lyrics screen with plain or syncronized lyrics |

## In depth about the different interfaces  
In this section we will discuss the interface in depth and give examples:  
### ExtensionClient  
An interface used by all extensions, you'll need to start with this interface no matter what your extension does  
Lets start by first extending the interface to you main class (in this example lets call it `MainClass`)  
```kotlin
import dev.brahmkshatriya.echo.common.clients.ExtensionClient

class MainClass :
    ExtensionClient {
    // Rest of the extension code here
}
```
Generally we'd write the interface on a different line because we will extend more interfaces besides this one  
As for the needed overrides, they provide the needed logic to create settings entries and a settings database sort of thing
- `onExtensionSelected()`: not a needed override, triggers when the USER selects the extension  
- `onInitialize()`: not a needed override, triggers when the extension is initialized  
- `settingItems`: NEEDED, sets the settings that you'll see in the extension setting menu  
This entry is a `List<Setting>` which can be pointed to an `emptyList()`  
This entry can take multiple types of setting items: `SettingTextInput()` `SettingSwitch()` `SettingSlider()` `SettingMultipleChoice()` `SettingList()` `SettingItem()` `SettingCategory()`  
We shall give a thorough example of all the items and their use  
**`SettingTextInput`**: A simlpe Text input entry  
This entry requires a **title** and a **key**, the key is what will be used as an identifier for the stored data, so that it can be queried and read, also includes an optional **summary** for info setting and **defaultValue**  
```kotlin
import dev.brahmkshatriya.echo.common.settings.SettingTextInput

val textInput= SettingTextInput(title= "title-for-the-input", key= "key-for-the-item-query", summary= "Some help info", defaultValue= "The Default Value")
override val settingItems: List<Setting> = listOf(textInput)
```
**`SettingSwitch()`**: An on/off togglable switch  
This entry requires a **title** and **key**, same as previous and the optional **summary** and **defaultValue**, the data stored is a **boolean**  
```kotlin
import dev.brahmkshatriya.echo.common,settings.SettingSwitch

val switchInput= SettingSwitch(title= "switch-title", key= "key-for-item-query", summary= "Some help info", defaultValue= false)
override val settingItems: List<Setting> = listOf(switchInput)
```
**`SettingSlide()`**: A slider to choose a numerical value  
This entry has:  
**title**: set the title of the slider  
**key**: the key of which the data is stored by  
**summary**: a summerization of what this slider is for, good for info not needed  
**defaultValue**: the default starting value, is an Int  
**from**: the beginning of the range  
**to**: wanna guess?  
**steps**: how much to advance in each slider step  
**allowOverride**: a boolean that decides if the user can use values outside the given range  
```kotlin
import dev.brahmkshatriya.echo.common.settings.SettingSlider

val sliderInput= SettingSlider(title= "some-title", key= "some-key", from= 0, to= 10, steps= 1)
override val settingItems: List<Setting> = listOf(sliderInput)
```
**`SettingOnClick`**: undocumented, don't use, seems to be used to run a `Unit` (function) when clicked  
This entry has:  
**title**, **key**, **summary**, **onClick**: the function that runs when clicked  
**`SettingMultipleChoice()`**: A multiple choice menu that the user can select multiple items from  
This entry has:  
**title**, **key**, **summary**  
**entryTitles**: A list of strings `List<String>` that holds the titles for the entries  
**entryValues**: A list of strings `List<String>` that holds the values for the entries (please note that the index should match with the previous param)  
**defaultEntryIndices**: A `Set<Int>` that contains the default selected indicies (aka default selected values)  
```kotlin
`import dev.brahmkshatriya.echo.common.settings.SettingMultipleChoice

val titles= listOf("title 1", "title 2")
val values= listOf("value 1", "value 2")
val multipleInput= SettingMultipleChoice(titile= "title", key= "some-key", entryTitles= titles, entryValues= values)
override val settingItems: List<Setting> = listOf(multipleInput)
```
**`SettingList()`**: A list of items that use use can select ONLY ONE of them, kinda like `SettingMultipleChoice` but single  
This entry has:  
The exact same params as `SettingMultipleChoice`  
```kotlin
import dev.brahmkshatriya.echo.common.settings.SettingList

val titles= listOf("title 1", "title 2")
val values= listOf("value 1", "value 2")
val listInput= SettingMultipleChoice(titile= "title", key= "some-key", entryTitles= titles, entryValues= values)
override val settingItems: List<Setting> = listOf(listInput)
```
**`SettingItem()`**: A label that shows the contents of a certain key, useful for showing the user info or for debug perhaps?  
This entry has:
**title**, **key**, **summary** (y'know the drill with these params)  
```kotlin
import dev.brahmkshatriya.echo.common.settings.SettingItem

val itemOutput= SettingItem(title= "Title", key= "some-key")
override val settingItems: List<Setting> = listOf(itemOutput)
```
**`SettingCategory()`**: A category containing a list of setting items, to group some settings together  
This entry has:  
**title**, **key**, **items**: a `List<Setting>` that contains a listOf() setting items  
- `setSettings()`: NEEDED, you dont need to worry about this, it just runs when the module is initialized to provide the actual settings provider  
```kotlin
private lateinit var setting: Settings // you use this and pass it to methods so that they can query the values of the keys and get settings values
override fun setSettings(settings: Settings) {
    setting= settings
}
```
- We shall also talk about accessing the data from the keys:  
This can be used in any part of the extension (usually to retrieve APIs or data the user inputs into settings)  
Let's say you want to create a dynamic base url system where the user has to first type the ip of the API for the extension to know where to direct the requests to  
After implementing the setting item you want to read what the user just typed in, so you'll pass the lazy loaded settings item to the method that handles your requests like so:  
In you main class:  
```kotlin
private lateinit var setting: Settings
override fun setSettings(settings: Settings) {
    setting = settings
}
val api by lazy { ApiService(setting) } // here we have an ApiService class that contains the function that handles requests

// ... 
// this is hypothetically inside some override that needs to perform an API request
val value= api.requestGet() //this will run the request and use the setting item initialized in the api lazy loaded class builder
```
In ApiService:  
```kotlin
class ApiService(setting: Settings) {
    BASEAPI = setting.getString(key= "base-api-key")
    fun requestGet() {}
}
```
To go more in depth about the available methods from the `Settings` class we will point all what's available:  
Available methods: `getString(key: String): String?` `putString(key: String, value: String?)` `getStringSet(key: String): Set<String>?` `putStringSet(key: String, value: Set<String>?)` `getInt(key: String): Int?` `putInt(key: String, value: Int?)` `getBoolean(key: String): Boolean?` `putBoolean(key: String, value: Boolean?)`  
We will NOT talk about them in detail, (please just read the method name and you'll understand immediately what it does)  
Querying and modifying keys will require a type, since the setting items themselves return a specific type (like the slider being an Int and the toggle being a bool), thus when querying or modifying a key you'll have to make sure it's the same type as the setting  
