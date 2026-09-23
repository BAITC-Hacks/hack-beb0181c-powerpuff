# EKT catalogue workspace

Existing EKT homepage is preserved. `/assistant` and `/cart` extend it for customers finding electrical products by name or article, inspecting source characteristics and warehouse stock, and reviewing a local demonstration cart.

The assistant currently uses catalogue API calls and deterministic messages. It does not call LLM or ML; free-form question understanding is not offered. Backend LLM code remains separate. Product information and documents come from EKT. Conflicting characteristics are shown without choosing a correct value. Similar names and recommendation links do not establish compatibility.

Adding requires a proposal and explicit confirmation. Sessions are per tab; expired sessions require a new-session action without replaying cart mutations. Cart positions now support absolute quantity edits and explicit deletion. Increasing quantity validates fresh EKT data; a changed price needs a separate confirmation. Decreasing and deleting use stored data without EKT. Versioned operations and idempotency prevent old proposals restoring deleted items. No supplier order or reservation is offered.

Currency, quantity units, sellable warehouses and purchase terms are not confirmed in current partner data. No real confirmed replacement pair or certificate-bearing sample was available for a positive browser check. Existing backend evidence and tests are documented separately.

Homepage refinement retains source content and imagery, adds a collapsible dark-blue assistant launcher, and connects the header search to the real catalogue. The launcher describes search without AI; WhatsApp remains available in the footer.
