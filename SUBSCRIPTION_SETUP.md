# Subscription Products Setup Guide

This guide walks you through creating your subscription products in **Google Play Console** and **RevenueCat**.

---

## Step 1: Create Products in Google Play Console

1. Go to [Google Play Console](https://play.google.com/console) > Your App > **Monetization setup**.
2. Make sure you have accepted the payments agreement if prompted.
3. Go to **Products** > **Subscriptions** (left menu).
4. Click **Create subscription**.

### Recommended Products:

| Product ID | Name | Price | Billing Period |
|------------|------|-------|----------------|
| `monthly_premium` | Monthly Premium | $4.99 | Monthly |
| `annual_premium` | Annual Premium | $39.99 | Yearly |
| `lifetime_premium` | Lifetime Access | $99.99 | One-time (see note) |

> **Note**: For "Lifetime", use **In-app products** (not Subscriptions) since it's a one-time purchase.

5. For each subscription:
   - Enter the **Product ID** exactly as shown above.
   - Set the **Name** and **Description**.
   - Add a **Base plan** with the price and billing period.
   - Click **Save** and then **Activate**.

---

## Step 2: Link Products to RevenueCat

1. Go to [RevenueCat Dashboard](https://app.revenuecat.com) > Your Project.
2. Click **Product catalog** (left menu).
3. Click **+ New** to add a product.
4. For each product:
   - **App**: Select "Contacts Cleaner (Play Store)"
   - **Product identifier**: Enter the **exact** Product ID from Play Console (e.g., `monthly_premium`)
   - Click **Add**.

---

## Step 3: Create an Offering

An "Offering" is a bundle of products shown to users.

1. In RevenueCat, go to **Offerings** (left menu).
2. Click **+ New Offering**.
3. **Identifier**: `default`
4. Add **Packages**:
   - Click **+ Add Package**.
   - **Identifier**: `$rc_monthly` (for monthly)
   - **Product**: Select `monthly_premium`
   - Repeat for annual (`$rc_annual`) and lifetime (`$rc_lifetime`).
5. Click **Save**.

---

## Step 4: Verify in App

Once set up, the app will automatically fetch these products from RevenueCat and display them in the Paywall.

**To test:**
1. Build and run the app.
2. Navigate to a premium action to trigger the Paywall.
3. Verify prices show correctly (not mock data).

---

## Step 5: Create Promo Codes (Optional)

After products are active:
1. Go to Play Console > **Monetize with Play** > **Promo codes**.
2. Click **Create promotion**.
3. Select your subscription product.
4. Generate codes to share with testers/influencers.
