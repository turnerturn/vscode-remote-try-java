import json
import uuid

def generate_unique_id():
    return str(uuid.uuid4())

def ensure_unique(data):
    seen_person_ids = set()
    seen_unids = set()
    seen_badge_numbers = set()

    for person in data:
        # Ensure unique personId
        while person["personId"] in seen_person_ids:
            person["personId"] = generate_unique_id()
        seen_person_ids.add(person["personId"])

        # Ensure unique unid
        while person["unid"] in seen_unids:
            person["unid"] = generate_unique_id()
        seen_unids.add(person["unid"])

        for badge in person["badges"]:
            # Ensure badge has a unid
            if "unid" not in badge:
                badge["unid"] = generate_unique_id()

            # Ensure unique badge number
            while badge["number"] in seen_badge_numbers:
                badge["number"] = generate_unique_id()
            seen_badge_numbers.add(badge["number"])

            # Ensure unique badge unid
            while badge["unid"] in seen_unids:
                badge["unid"] = generate_unique_id()
            seen_unids.add(badge["unid"])

            for access_level in badge["accessLevels"]:
                # Ensure access level has a unid
                if "unid" not in access_level:
                    access_level["unid"] = generate_unique_id()

                # Ensure unique access level unid
                while access_level["unid"] in seen_unids:
                    access_level["unid"] = generate_unique_id()
                seen_unids.add(access_level["unid"])

    return data

# Load JSON data
with open('/workspaces/vscode-remote-try-java/src/test/resources/cpam-persons-sample.json', 'r') as file:
    data = json.load(file)

# Ensure uniqueness
unique_data = ensure_unique(data)

# Save the updated JSON data
with open('/workspaces/vscode-remote-try-java/src/test/resources/cpam-persons-sample-unique.json', 'w') as file:
    json.dump(unique_data, file, indent=2)

print("Unique JSON data has been saved to cpam-persons-sample-unique.json")