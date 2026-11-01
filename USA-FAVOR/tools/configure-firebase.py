#!/usr/bin/env python3
"""Convert public google-services.json files to manual FirebaseOptions assets."""
import argparse,json,pathlib,shutil
p=argparse.ArgumentParser();p.add_argument('--admin-json',required=True,type=pathlib.Path);p.add_argument('--user-json',required=True,type=pathlib.Path);p.add_argument('--database-url',help='Use if firebase_url is absent from downloaded files');args=p.parse_args();root=pathlib.Path(__file__).resolve().parents[1]
for project,package,source in [('USA-Admin','com.usa.admin',args.admin_json),('USA-Favor','com.usa.user',args.user_json)]:
 data=json.loads(source.read_text());info=data['project_info'];client=next(c for c in data['client'] if c['client_info']['android_client_info']['package_name']==package)
 config={'projectId':info['project_id'],'databaseUrl':args.database_url or info.get('firebase_url',''),'apiKey':client['api_key'][0]['current_key'],'applicationId':client['client_info']['mobilesdk_app_id']}
 if not config['databaseUrl'].startswith('https://'):raise SystemExit('Create RTDB first or supply --database-url with your HTTPS database URL')
 app=root/project/'app';(app/'src/main/assets/firebase-config.json').write_text(json.dumps(config,indent=2)+'\n')
 if source.resolve()!=(app/'google-services.json').resolve():shutil.copyfile(source,app/'google-services.json')
 print(project+': public Firebase configuration saved (values not printed)')
