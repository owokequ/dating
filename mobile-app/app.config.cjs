const fs = require("node:fs");
const path = require("node:path");

const config = require("./app.json");
const googleServicesFile = path.join(__dirname, "google-services.json");

// Expo Go does not need FCM configuration. EAS automatically includes it in
// Android builds as soon as the Firebase configuration file is present.
if (fs.existsSync(googleServicesFile)) {
  config.expo.android.googleServicesFile = "./google-services.json";
}

module.exports = config;
