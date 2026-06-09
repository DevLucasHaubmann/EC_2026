package com.tukan.api.dto;

import jakarta.validation.constraints.Size;

// URL scheme validation (http/https only; rejects javascript:, data:, file:, no-scheme)
// is enforced in MeService.updateAvatar — kept there because URI parsing is more robust
// than a Bean Validation regex and a blank value is a valid "clear avatar" request.
public record UpdateAvatarRequest(

        @Size(max = 512, message = "A URL do avatar deve ter no máximo 512 caracteres.")
        String avatarUrl
) {
}
