# Real-Time Developer Notifications (RTDN) Setup

This configures Google to instantly tell RevenueCat when a purchase happens.

### Step 1: Enable Cloud Pub/Sub API
1.  Go to [Google Cloud Console](https://console.cloud.google.com/).
2.  Search for **"Cloud Pub/Sub API"** in the top bar.
3.  Click **Enable**.

### Step 2: Create a Topic
1.  Go to **Pub/Sub** > **Topics** (search for "Topics" if you can't find it).
2.  Click **CREATE TOPIC**.
3.  **Topic ID**: `revenuecat-notifications` (or similar).
4.  Uncheck "Add a default subscription".
5.  Click **Create**.

### Step 3: Grant Access to RevenueCat
1.  After creating, you will see the topic details page.
2.  Click the **Permissions** tab (or "Show Info Panel" on the right).
3.  Click **ADD PRINCIPAL**.
4.  **New principals**: `google-play-developer-notifications@system.gserviceaccount.com`
5.  **Role**: Select **Pub/Sub Publisher**.
6.  Click **Save**.

### Step 4: Link to Play Console
1.  Copy your **Topic name** (it looks like `projects/YOUR_PROJECT_ID/topics/revenuecat-notifications`).
2.  Go to [Google Play Console](https://play.google.com/console) > **Monetization setup**.
3.  Scroll to **Real-time developer notifications**.
4.  Paste the **Topic name** into the box.
5.  Click **Send test notification**. (It should say "Test notification sent").
6.  Click **Save changes**.

### Step 5: Link to RevenueCat
1.  Go back to **RevenueCat** > **App Settings**.
2.  In the "Google developer notifications" section:
    *   **Topic ID**: Enter just the ID: `projects/YOUR_PROJECT_ID/topics/revenuecat-notifications`
3.  Click **Connect to Google**.
    *   *Note: If it asks to sign in, use the Google account that owns the Cloud Project.*

Done! Purchases are now instant.
