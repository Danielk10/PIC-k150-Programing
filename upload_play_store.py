import argparse
import sys
from google.oauth2.service_account import Credentials
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

def main():
    parser = argparse.ArgumentParser(description="Upload AAB to Google Play Store")
    parser.add_argument('--package_name', required=True, help='The package name of the app')
    parser.add_argument('--aab_path', required=True, help='Path to the .aab file')
    parser.add_argument('--service_account_json', required=True, help='Path to the service account JSON file')
    parser.add_argument('--track', default='production', help='The track to release on (e.g., production, beta, alpha, internal)')
    parser.add_argument('--release_notes', default='', help='Structured release notes for Spanish (es-419 and es-ES) using multiline bullet points (- Item)')
    parser.add_argument('--release_notes_en', default='', help='Structured release notes for English (en-US) using multiline bullet points (- Item)')
    
    args = parser.parse_args()

    scopes = ['https://www.googleapis.com/auth/androidpublisher']
    credentials = Credentials.from_service_account_file(args.service_account_json, scopes=scopes)
    service = build('androidpublisher', 'v3', credentials=credentials)

    print(f"Creating an edit for package {args.package_name}...")
    edit_request = service.edits().insert(body={}, packageName=args.package_name)
    result = edit_request.execute()
    edit_id = result['id']

    try:
        print(f"Uploading AAB {args.aab_path}...")
        media = MediaFileUpload(args.aab_path, mimetype='application/octet-stream', resumable=True)
        bundle_response = service.edits().bundles().upload(
            editId=edit_id,
            packageName=args.package_name,
            media_body=media
        ).execute()

        version_code = bundle_response['versionCode']
        print(f"Successfully uploaded AAB with version code: {version_code}")

        print(f"Assigning AAB to track '{args.track}'...")
        
        release_notes_list = []
        if args.release_notes:
            release_notes_list.append({'language': 'es-419', 'text': args.release_notes})
            release_notes_list.append({'language': 'es-ES', 'text': args.release_notes})
        
        en_notes = args.release_notes_en if args.release_notes_en else args.release_notes
        if en_notes:
            release_notes_list.append({'language': 'en-US', 'text': en_notes})

        track_release = {
            'track': args.track,
            'releases': [{
                'name': f"Release {version_code}",
                'versionCodes': [version_code],
                'status': 'completed',
                'releaseNotes': release_notes_list
            }]
        }

        service.edits().tracks().update(
            editId=edit_id,
            packageName=args.package_name,
            track=args.track,
            body=track_release
        ).execute()

        print("Committing the edit...")
        service.edits().commit(editId=edit_id, packageName=args.package_name).execute()
        print("Release successfully published to Google Play!")

    except Exception as e:
        print(f"An error occurred: {e}")
        print("Reverting the edit...")
        service.edits().delete(editId=edit_id, packageName=args.package_name).execute()
        sys.exit(1)

if __name__ == '__main__':
    main()
