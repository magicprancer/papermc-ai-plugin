# PaperMC AI Plugin (Enhanced)

This enhanced package includes:
- Function schemas sent to the model to encourage structured function calls
- Admin audit logging (audit.log in plugin data folder)
- Basic safety filters (blacklist patterns, whitelist targets)
- NPC memory (per-NPC recent chat stored in memory and used as context)

## How to use
1. Set `openai_api_key` in `src/main/resources/config.yml` or set `OPENAI_API_KEY`.
2. Tune `ai_admin_whitelist_targets`, `ai_admin_allowed_commands`, and `ai_admin_max_actions_per_minute`.
3. Build with `mvn clean package`.
4. Drop the resulting jar into your Paper server `plugins/` folder.

## Notes
- The plugin uses the Chat Completions endpoint and provides simple function schemas to the model. Always review audit.log after testing.
- NPCs can request actions but these are permission-gated and audited.
