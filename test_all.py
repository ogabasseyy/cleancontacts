def check_tostring(filepath, class_name):
    with open(filepath, 'r') as f:
        content = f.read()
    if f"fun toString(): String {{\n        return \"{class_name}" in content:
        print(f"✅ {class_name} has toString")
    else:
        print(f"❌ {class_name} missing toString")

check_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/domain/model/Contact.kt', 'AccountGroupSummary')
check_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/data/api/FeedbackApi.kt', 'FeedbackRequest')
check_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/data/api/WhatsAppDetectorApi.kt', 'SessionStatus')
check_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/data/api/WhatsAppDetectorApi.kt', 'PairingRequest')
check_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/data/api/WhatsAppDetectorApi.kt', 'CheckNumbersRequest')
check_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/data/api/WhatsAppDetectorApi.kt', 'NumberCheckResult')
check_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/data/api/WhatsAppDetectorApi.kt', 'BatchCheckRequest')
check_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/data/api/WhatsAppDetectorApi.kt', 'WhatsAppContact')
check_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/data/api/WhatsAppDetectorApi.kt', 'BusinessProfile')
