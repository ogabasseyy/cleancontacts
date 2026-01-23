# RevenueCat & Google Play Setup Guide

## ✅ Package Name Confirmed
Your app is now built as **`com.ogabassey.contactscleaner`**.
This matches your RevenueCat configuration.

---

## How to Create the Service Account JSON
RevenueCat needs this file to talk to Google servers to verify purchases.

### Step 1: Open Google Cloud Console
1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Select your project (the one linked to your Google Play Developer account).
   * *If you don't have one, create a new project.*

### Step 2: Enable Google Play Admin API
1. In the search bar at the top, type **"Google Play Android Developer API"**.
2. Click **Enable**.

### Step 3: Create Service Account
1. Go to **IAM & Admin** > **Service Accounts**.
2. Click **+ CREATE SERVICE ACCOUNT**.
3. **Name**: `revenuecat-upload` (or similar).
4. Click **Create and Continue**.
5. **Role**: Select **Service Accounts** > **Service Account User**.
   * *Also add* **Pub/Sub Admin** if you plan to use Real-Time Developer Notifications (optional for now).
6. Click **Done**.

### Step 4: Generate Key (The JSON File)
1. Click on the email address of the service account you just created.
2. Go to the **Keys** tab.
3. Click **Add Key** > **Create new key**.
4. Select **JSON**.
5. Click **Create**.
6. The file will download to your computer. **This is the file you drop into RevenueCat.**

### Step 5: Grant Access in Google Play Console
1. Go to [Google Play Console](https://play.google.com/console).
2. Go to **Users and permissions** (left menu).
3. Click **Invite new users**.
4. Paste the **email address** of the service account you created:
   `revenuecat-upload@contacts-cleaner-485120.iam.gserviceaccount.com`
5. **Permissions**:
   * Go to "App permissions" > "Add app" > Select your app > Apply.
   * Check **View app information** and **Manage orders and subscriptions**.
   * *Critical*: Ensure **View financial data** is checked if available (sometimes under Account permissions).
6. Click **Invite user**.

Now you can upload that JSON file to RevenueCat!
