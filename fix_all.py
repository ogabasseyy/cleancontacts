import re

def insert_tostring(filepath, class_name, fields_str, redacted_fields=None):
    if redacted_fields is None:
        redacted_fields = []

    with open(filepath, 'r') as f:
        content = f.read()

    pattern = r'(data\s+class\s+' + class_name + r'\s*\([^)]*\))(\s*\{)?'
    match = re.search(pattern, content)
    if not match:
        print(f"Could not find {class_name} in {filepath}")
        return content

    matched_string = match.group(0)

    props = []
    for f_name in fields_str:
        if f_name in redacted_fields:
            if "List" in f_name or "numbers" == f_name:
                props.append(f"{f_name}=[***REDACTED (size=${{{f_name}.size}})***]")
            else:
                props.append(f"{f_name}=***REDACTED***")
        else:
            props.append(f"{f_name}=${f_name}")

    tostring_body = f'return "{class_name}({", ".join(props)})"'

    tostring_method = f""" {{
    // 2026 Security Fix: Override toString to prevent accidental logging of PII (CWE-532)
    override fun toString(): String {{
        {tostring_body}
    }}
}}"""

    if "override fun toString(): String" not in content[match.start():match.end()+200]:
        new_content = content.replace(match.group(1), match.group(1) + tostring_method)
        with open(filepath, 'w') as f:
            f.write(new_content)
        print(f"Updated {class_name} in {filepath}")
    else:
        print(f"{class_name} already has toString in {filepath}")

insert_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/domain/model/Contact.kt',
                'AccountGroupSummary',
                ['accountType', 'count', 'accountName'],
                ['accountName'])

insert_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/data/api/FeedbackApi.kt',
                'FeedbackRequest',
                ['category', 'deviceInfo', 'email', 'message'],
                ['email', 'message'])

insert_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/data/api/WhatsAppDetectorApi.kt',
                'SessionStatus',
                ['connected', 'userId', 'lastActivity', 'createdAt', 'contactsCount', 'businessDetectionProgress', 'error', 'phoneNumber'],
                ['phoneNumber'])

insert_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/data/api/WhatsAppDetectorApi.kt',
                'PairingRequest',
                ['phoneNumber'],
                ['phoneNumber'])

insert_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/data/api/WhatsAppDetectorApi.kt',
                'CheckNumbersRequest',
                ['numbers'],
                ['numbers'])

insert_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/data/api/WhatsAppDetectorApi.kt',
                'NumberCheckResult',
                ['hasWhatsApp', 'number', 'jid'],
                ['number', 'jid'])

insert_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/data/api/WhatsAppDetectorApi.kt',
                'BatchCheckRequest',
                ['batchSize', 'delayMs', 'numbers'],
                ['numbers'])

insert_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/data/api/WhatsAppDetectorApi.kt',
                'WhatsAppContact',
                ['isBusiness', 'businessProfile', 'jid', 'phoneNumber', 'name', 'pushName'],
                ['jid', 'phoneNumber', 'name', 'pushName'])

insert_tostring('shared/src/commonMain/kotlin/com/ogabassey/contactscleaner/data/api/WhatsAppDetectorApi.kt',
                'BusinessProfile',
                ['category', 'website', 'description', 'email', 'address'],
                ['description', 'email', 'address'])
