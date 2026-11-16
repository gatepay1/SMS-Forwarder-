# GatePay SMS Forwarder

GatePay মার্চেন্টদের ফোনে আসা bKash / Nagad / Rocket / Upay পেমেন্ট এসএমএস স্বয়ংক্রিয়ভাবে যাচাই করে সার্ভারে পাঠায়।

## ফিচার
- Gate›Pay **SMS** স্প্ল্যাশ অ্যানিমেশন
- `smsd_…` টোকেন কপি-পেস্ট (QR নেই)
- সিম যোগের সময় SIM 1/2 + Personal / Agent / Merchant নির্বাচন, ১১ ডিজিট নম্বর যাচাই
- ফোরগ্রাউন্ড সার্ভিস (`START_STICKY`, `stopWithTask=false`) — রিসেন্ট থেকে সরালেও চলে
- ফোন রিস্টার্টে নিজে চালু, ইন্টারনেট না থাকলে কিউতে রেখে পরে পাঠায়
- ব্যাটারি সীমাবদ্ধতা বন্ধের বাটন, অনলাইন/অফলাইন স্ট্যাটাস ও কার্যক্রম লগ

## APK পাওয়া
GitHub-এ পুশ করলে **Actions → Build APK** নিজে চলে; শেষে Artifacts থেকে `GatePaySMS-apk` ডাউনলোড করুন।
লোকালি: Android Studio দিয়ে ফোল্ডারটি খুলুন → Build → Build APK.
