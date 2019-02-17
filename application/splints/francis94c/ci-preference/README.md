# ci-preference

The ci-preference library is a library that allows you store values in a key-value pair manner. the values could be a string, integer, or an array.

To install and use in your Code Igniter project, download and install [Splint](https://splint.cynobit.com), then open a terminal at the root of your intended Code Igniter distribution and run:

```bash
splint install francis94c/ci-preference
```

To load and use:

```php
$params = array();
$params["file_name"] = "preferences"; // File name of the preference file.
$this->load->splint("francis94c/ci-preference", "+CIPreferences", $params, "alias");
$this->alias->get("key", "defaultVal");
$this->alias->set("key", "val");
$this->alias->commit(); // Commits all values to file.
```

