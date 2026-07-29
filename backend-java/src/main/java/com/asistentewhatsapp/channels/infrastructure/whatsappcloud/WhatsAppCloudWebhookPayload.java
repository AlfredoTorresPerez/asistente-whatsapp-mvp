package com.asistentewhatsapp.channels.infrastructure.whatsappcloud;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record WhatsAppCloudWebhookPayload(String object, List<Entry> entry) {

	public record Entry(String id, List<Change> changes) {
	}

	public record Change(String field, Value value) {
	}

	public record Value(@JsonProperty("messaging_product") String messagingProduct, Metadata metadata,
			List<Contact> contacts, List<Message> messages, List<Status> statuses) {
	}

	public record Metadata(@JsonProperty("display_phone_number") String displayPhoneNumber,
			@JsonProperty("phone_number_id") String phoneNumberId) {
	}

	public record Contact(Profile profile, @JsonProperty("wa_id") String waId) {
	}

	public record Profile(String name) {
	}

	public record Message(String from, String id, String timestamp, Text text, Interactive interactive, Button button,
			Image image, Document document, Audio audio, Video video, Sticker sticker, Location location,
			Contacts contacts, Context context, String type) {

		public record Text(String body) {
		}

		public record Interactive(@JsonProperty("button_reply") ButtonReply buttonReply,
				@JsonProperty("list_reply") ListReply listReply, String type) {
		}

		public record ButtonReply(String id, String title) {
		}

		public record ListReply(String id, String title, String description) {
		}

		public record Button(String payload, String text) {
		}

		public record Image(String id, @JsonProperty("mime_type") String mimeType, String sha256, String caption) {
		}

		public record Document(String id, @JsonProperty("mime_type") String mimeType, String sha256, String caption,
				String filename) {
		}

		public record Audio(String id, @JsonProperty("mime_type") String mimeType, String sha256, Boolean voice) {
		}

		public record Video(String id, @JsonProperty("mime_type") String mimeType, String sha256, String caption) {
		}

		public record Sticker(String id, @JsonProperty("mime_type") String mimeType, String sha256, String animated) {
		}

		public record Location(double latitude, double longitude, String name, String address) {
		}

		public record Contacts(List<ContactItem> contacts) {
		}

		public record ContactItem(List<PhoneEntry> phones, List<NameEntry> names) {
		}

		public record PhoneEntry(String phone, @JsonProperty("wa_id") String waId, String type) {
		}

		public record NameEntry(@JsonProperty("formatted_name") String formattedName,
				@JsonProperty("first_name") String firstName, @JsonProperty("last_name") String lastName) {
		}

		public record Context(String from, String id, @JsonProperty("forwarded") Boolean forwarded,
				@JsonProperty("frequently_forwarded") Boolean frequentlyForwarded) {
		}
	}

	public record Status(String id, String status, String timestamp, @JsonProperty("recipient_id") String recipientId,
			Conversation conversation, List<ErrorInfo> errors, Pricing pricing) {
	}

	public record Conversation(String id, @JsonProperty("expiration_timestamp") String expirationTimestamp,
			Origin origin) {
	}

	public record Origin(String type) {
	}

	public record ErrorInfo(int code, String title, String message,
			@JsonProperty("error_data") java.util.Map<String, Object> errorData, String href,
			@JsonProperty("fbtrace_id") String fbtraceId) {
	}

	public record Pricing(boolean billable, @JsonProperty("pricing_model") String pricingModel, String category) {
	}
}
