# 02: Pure Domain vCard Exporter & Share Action

**What to build:** Build a pure Kotlin `VCardExporter` utility that converts contacts into standard vCard 3.0 syntax (`BEGIN:VCARD` ... `END:VCARD`) with names, phones, emails, organizations, roles, and notes. Connect it to the Share intent on the Relationship Tab, generating a single combined `.vcf` file and opening the Android Share Sheet via `Intent.ACTION_SEND` with MIME type `text/x-vcard`.

**Blocked by:** 01: Core Selection State, Long-Press Trigger & Decoupled Card Geometry

**Status:** done

- [x] `VCardExporter.generateCombinedVCard(contacts)` generates valid, RFC-compliant vCard 3.0 representations for single and multiple contacts.
- [x] Special characters and multiline notes are escaped properly according to vCard standards.
- [x] Pure Kotlin unit tests in `VCardExporterTest` assert exact vCard outputs for various contact configurations.
- [x] Tapping Share creates a temporary combined `.vcf` file and launches `Intent.ACTION_SEND` (`text/x-vcard`).
