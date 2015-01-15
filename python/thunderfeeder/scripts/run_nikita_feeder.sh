#!/bin/bash
IMAGING_INPUT_DIR=/groups/ahrens/ahrenslab/Nikita/Realtime/imaging/
EPHYS_INPUT_DIR=/groups/ahrens/ahrenslab/Nikita/Realtime/ephys/
SPARK_OUTPUT_DIR=/nobackup/freeman/streaminginput/
# TMP_OUTPUT_DIR must be on the same filesystem as SPARK_OUTPUT_DIR:
TMP_OUTPUT_DIR=/nobackup/freeman/streamingtmp/
THUNDER_STREAMING_DIR=/groups/freeman/home/swisherj/thunder-streaming
MAX_FILES=-1  # disable rate limiting

# local testing directories - leave commented out for use on cluster:
# IMAGING_INPUT_DIR=/mnt/data/data/nikita_mock/imginput/
# EPHYS_INPUT_DIR=/mnt/data/data/nikita_mock/behavinput/
# SPARK_OUTPUT_DIR=/mnt/tmpram/sparkinputdir/
# # TMP_OUTPUT_DIR must be on the same filesystem as SPARK_OUTPUT_DIR:
# TMP_OUTPUT_DIR=/mnt/tmpram/
# THUNDER_STREAMING_DIR=/mnt/data/src/thunder_streaming_mainline_1501
# MAX_FILES=2  # turn on rate limiting for simulated runs

export TMP=$TMP_OUTPUT_DIR
rm $SPARK_OUTPUT_DIR/*

$THUNDER_STREAMING_DIR/python/thunderfeeder/grouping_series_stream_feeder.py \
$IMAGING_INPUT_DIR  $EPHYS_INPUT_DIR  $SPARK_OUTPUT_DIR \
--prefix-regex-file $THUNDER_STREAMING_DIR/resources/regexes/nikita_queuenames.regex \
--timepoint-regex-file $THUNDER_STREAMING_DIR/resources/regexes/nikita_timepoints.regex \
--max-files $MAX_FILES --check-size
