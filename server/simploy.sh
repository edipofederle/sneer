# Allow running lein as root
export LEIN_ROOT=yes

# Build new server version
lein do clean, midje, uberjar || exit -1

mkdir -p /root/sneer-live      || exit -1
mkdir -p /root/sneer-live/logs || exit -1

echo REPLACE UBERJAR
mv target/uberjar/*standalone.jar /root/sneer-live/. || exit -1

echo PKILL OLD SERVER PROCESS
pkill -f sneer.server-0.1.0-SNAPSHOT-standalone.jar

cd /root/sneer-live

# Append date to previous log filename
mv logs/log "logs/log-`date +%Y-%m-%d_%H-%M`"

echo START NEW SERVER PROCESS
SERVER_PORT=5555
java -jar ./sneer.server-0.1.0-SNAPSHOT-standalone.jar $SERVER_PORT  &> logs/log&
echo DONE
