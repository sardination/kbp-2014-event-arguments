This is the archived README for the 2014 evaluation.  Some paths, etc. may be out of date.

This is code developed by BBN to support the 2015 TAC KBP Event Argument Linking Shared task and the 
[2014 TAC KBP Event Argument Shared Task](http://www.nist.gov/tac/2014/KBP/Event/index.html). 
A draft of the description of this task may be found [here](https://docs.google.com/document/d/1NRrRciiPMEZfqdjXEljyzWn-Zlw-jEm0PBqT-t1owJ0/edit?usp=sharing).
A draft of the 2015 task description will be available soon.

This repository contains three artifacts: 
* `kbp-events2014` contains classes to represent system responses, assessments, and response linkings for
the task. While these are mainly to support the executables in this repository,
if your system is based on Java or another JVM language (including Python via Jython), you are strongly
encouraged to use them.
* `kbp-events2014-scorer` contains the scoring code (but not scoring binary).
* `kbp-events2014-bin` contains all the executable programs: the validator, the pooler, the scorer, etc.

## Building 

Requirements:
* [Maven](http://maven.apache.org/)

Build steps:
* Check out the [`bue-common-open`](https://github.com/rgabbard-bbn/bue-common-open) repository
and do `mvn install` from its root.  
* Do `mvn install` from the root of this repository.
* do `chmod +x kbp-events2014-bin/target/appassembler/bin/*` (you only need to do this the first time)

## Using
### System Output Stores and Annotation Stores
A _system output store_ represents the output for a KBP Event Argument system on
a collection of documents. It consists of a directory containing exactly one
file per input document, named by the docID. The internal format of these files
is described in the task specification linked to above.

An _annotation store_ contains assessments of the answers from a system output
store.  Its format is the same as a system output store except within the files
there are additional assessment columns, as described in the task specification.

### Evaluation Workflow

The following workflow will be used during the pilot and (unless changes are made) 
real evaluations.  All executables referenced below may be found in 
`kbp-events2014-bin/target/appassembler/bin`.

* a 'quote filter' to remove material with CAS and BF offsets in quoted regions
will be built from the original text of the data set.
* competitor submissions will be validated using `validateSystemOutput`.
* all material from quoted regions will be removed from competitor submissions.
* all submissions will be combined into a single system output store using 
`poolSystemOutput`.
* this combined system output store will be transformed into an annotation store
using `importSystemOutputToAnnotationStore`.
* LDC annotators will assess this annotation store.
* All competitor submissions will be evaluated against the complete annotation
store using `kbpScorer`.

### Running the demo
We have included a demo showing the full annotation workflow.
The demo requires you to have a copy of the ACE event training data. For brevity,
the following instructions assume `kbp-events2014-bin/target/appassembler/bin`
has been added to your system path. All paths are relative to the root of your
working copy.

* Edit `sample/params/root.params` to point to your working copy of this repository.
* Edit `sample/docIdToOriginalText.txt` to point to the files in your copy
* Edit the path in `sample/storesToPool.txt` to point to your working copy.
of the ACE event training data. Note the specified directory will not yet exist.
* Run `buildQuoteFilter sample/params/buildQuoteFilter.params`
* Run `validateSystemOutput sample/params/validate.params`
* Run `applyQuoteFilter sample/params/applyQuoteFilter.params`. These documents
don't actually have any quotes, so don't expect to see anything filtered.
* Run `poolSystemOutput sample/params/pool.params`. In this case we only have,
one system output store, so this is a trivial pooling.
* Run `importSystemOutputToAnnotationStore sample/params/importToAnnotationStore.params`.
This will show you what sort of input the LDC assessors will get.
* We've provided an answer key for demo purposes. To score the sample output
against it, run `kbpScorer sample/params/score.params`.  Note this answer key 
is automatically derived from ACE annotation and is not guaranteed to be either
correct or complete. The scorer will write various scores and logs to the 
`sample/scoringObserverLogs` directory. `Standard/Aggregate` contains what is
probably most important, the overall score according to the standard metric.

### Parameter Files
Most of the executables take parameter files as input.  These have the format
```
key1: value1
# this is a comment!
key2: value2
```

### `validateSystemOutput`
This program will check that your submission:
* has the correct format
* contains only legal event roles and types

If either of these fail, the program will halt with an error message. In the future,
we plan to add enforcement of rules concerning what portions of answers may
come from within `<quote>` regions.

Additionally, this program will dump to standard output a human-readable version
of your responses with all offsets resolved to strings from the original documents
so that you can check for mistakes.  

This program takes the following parameters:
* `systemOutputStore`: the path of the system output store to be validated 
* `validRoles`: is `data/2014.types.txt` (for KBP 2014)
* `dump`: whether to dump response to `stdout` in a human readable format.
* `docIDMap`: (required if `dump` is `true`) a list of tab-separated pairs of doc ID and path to the 
them to standard output.


### `poolSystemOutput`
Combines the system output from multiple systems into a single system output store.

Parameters:
* `storesToPool`: a file listing paths to stores to pool, one per line
* `pooledStore`: the location to write the pooled store to
* `addMode`: either `CREATE` to create a new store for output (overwriting anything 
currently there) or `ADD` to append to an existing store.

### `importSystemOutputToAnnotationStore`
Turns a system output store into an annotation store ready for LDC's annotators.

Parameters:
* `systemOutput`: system output to import
* `annotationStore`: location to create annotation store. The program will 
refused to create a new annotation store over an existing, non-empty one.

### `kbpScorer`
Scores system output against an annotation store.

Parameters:
* `annotationComplete`: specifies whether to check that there are no unannotated
tuples in the annotation store.  For the evaluation, this will always be `true`.
* `scoringOutput`: where to write various scoring log files.
* `systemOutput`: the system output store to score
* `answerKey`: the answer key to score against.

## Questions
### How can I use the `Response`, etc. in my system's code?
Add the following to the `dependencies` section of your project's `pom.xml` (or take similar steps if using Gradle, etc.):
```
<dependency>
      <groupId>com.bbn.kbp.events2014</groupId>
      <artifactId>kbp-events2014</artifactId>
      <version>1.0.0-SNAPSHOT</version>
</dependency>
```
This artifact is not deployed to Maven Central, so you will need to
install it in your local repository as described above.

### How can I used the LDC's assessment of the pilot with this code?
The LDC's pilot assessment's format differs from the format and constraints expected by this code in a few ways. 
Follow these steps to transform it into a usable annotation store:

1. Remove the `.out` extensions from the assessment files
```
cd LDC2014E40_TAC_2014_KBP_Event_Argument_Extraction_Pilot_Assessment_Results/data/LDC_assessments
rename .out "" *.out
```
2. Run the repair program 
```
./kbp-events2014-bin/target/appassembler/bin/repairAnnotationStore repair.params
```

where repair.params looks like

```
randomSeed: 0
pathToWriteFixedStore: the path you want the repaired annotation store written to
brokenStore:  /nfs/mercury-04/u10/kbp/pilot/assessment/LDC2014E40_TAC_2014_KBP_Event_Argument_Extraction_Pilot_Assessment_Results/data/LDC_assessments          
```

Of course, alter the path in `brokenStore` to wherever you are storing the pilot assessment.

### Given participant submissions and the LDC's assessment, how do I score?

Note that only NIST can actually do this, since only they have access to all the pilot submissions.

1. create a file in the `params` subdirectory called `kbpRepoPath.params`. In it put the following:
```
kbpRepoPath: path to your working copy of this repository
```
2. Set the following environmental variables:
```
KBPOPENREPO=path to working copy of kbp-2014-event-arguments
PARTICIPANTS=path of a copy of KBP2014_event-argument-pilot_runs_20140421.tgz
ASSESSMENTS=path of a copy of LDC2014E40_TAC_2014_KBP_Event_Argument_Extraction_Pilot_Assessment_Results_V1.1.tgz 
```
3. Run `bin/evaluatePilot.sh`
4. Output will be under `output/pilot`. A summary score file will be written to a path printed at the end of the script.

## Contact
For questions concerning the software, please contact `rgabbard@bbn.com`.  If you 
have bugs or feature requests, you can use the GitHub Issue Tracker.


