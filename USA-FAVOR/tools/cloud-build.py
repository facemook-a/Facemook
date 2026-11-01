#!/usr/bin/env python3
"""Use retained cloud tools without storing credentials or network proxy values."""
import os,pathlib,subprocess,sys,urllib.parse
root=pathlib.Path(__file__).resolve().parents[1];env=os.environ.copy();jdk=pathlib.Path('/workspace/toolchains/jdk-21.0.9')
if jdk.exists():env['JAVA_HOME']=str(jdk)
env.setdefault('GRADLE_USER_HOME','/workspace/toolchains/gradle-home');env.setdefault('ANDROID_HOME','/workspace/android-sdk')
proxy=env.get('HTTPS_PROXY')
opts=env.get('GRADLE_OPTS','')
if proxy:
 u=urllib.parse.urlparse(proxy)
 if u.hostname and not u.username:
  opts+=f' -Dhttps.proxyHost={u.hostname} -Dhttps.proxyPort={u.port or 8080} -Dhttp.proxyHost={u.hostname} -Dhttp.proxyPort={u.port or 8080}'
if pathlib.Path('/etc/ssl/certs/java/cacerts').exists():opts+=' -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts'
env['GRADLE_OPTS']=opts
cmd=[str(root/'gradlew')]+(sys.argv[1:] or [':Admin:assembleDebug',':User:assembleDebug',':Admin:testDebugUnitTest',':User:testDebugUnitTest'])
sys.exit(subprocess.call(cmd,cwd=root,env=env))
