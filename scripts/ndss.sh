for var in Asroot  FakePlayer  Gone60  GPSSMSSpy  HippoSMS  RogueSPPush  SndApps  YZHC  zHash  Zsone
do
    echo '=========Generating signatures for ' $var
    ./generation.sh samples/popl17/ndss16/$var/ 2 10 60 experiments/output
    mkdir experiments/$var
    mv experiments/output/*.json experiments/$var

    echo '=========Testing signatures for ' $var
    for f in samples/popl17/ndss16/*; do
      echo "NDSS -------------------------------> $f"
      ./accuracy.sh experiments/$var $f
    done

    for f in samples/popl17/fse14/*; do
      echo "FSE -------------------------------> $f"
      ./accuracy.sh experiments/$var $f
    done

    echo '*********************************************'
done

