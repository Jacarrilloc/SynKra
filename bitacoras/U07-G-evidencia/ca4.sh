export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-21-amd64-linux.2
echo "fecha: $(date -Is)  JAVA_HOME=$JAVA_HOME"
f=aidlc-docs/construction/U07-despliegue/code/summary.md
echo '$ grep -c BUILD SUCCESSFUL summary.md'; grep -c 'BUILD SUCCESSFUL' "$f"
echo '$ grep -c salida=0 summary.md'; grep -c 'salida=0' "$f"
echo '$ ls evidencia/'; ls aidlc-docs/construction/U07-despliegue/code/evidencia/
echo '$ head -3 evidencia/*.txt'; head -3 aidlc-docs/construction/U07-despliegue/code/evidencia/*.txt | cut -c1-200
echo '$ git log -1 --format=%h -- backend'; git log -1 --format=%h -- backend
