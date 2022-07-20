# Inputs and their format for CausalPath

Users need to prepare a minimum of 2 text files to run a CausalPath analysis.

1. Proteomics data
2. Analysis parameters

An example set of input files is available [here](https://github.com/PathwayAndDataAnalysis/causalpath/raw/master/wiki/sample-data-and-parameters.zip).

## Proteomics data

CausalPath reads the proteomics dataset from a tab-delimited text file where the first row contains the column headers. Below are description of those columns.

**ID:** A unique text identifier for each row in the dataset. Ideally, it is better if the ID contains the gene symbols and modification sites if applicable. Those IDs are displayed in the tooltip text of related network parts by visualization software. Make sure that IDs do not contain the characters "|" and "@". Otherwise ChiBE won't be able to display them.

**Symbols:** HGNC symbol of the related gene. If there are more than one gene symbol reltaed to this row, there should be a single space between each symbol.

**Sites:** If the row contains a phosphoprotein measurement, this column should indicate protein sites that are affected. The format for the site is one letter capital aminoacid code, following an integer for the location on the UniProt caconical sequence, such as `Y142`, or `S78`. When there is more than one related site, they should be separated with a pipe (`|`), such like `S151|T153`, to form a site group. If the row is related to more than one gene symbol, there should exists a site group for each symbol, separated with a single space.

**Effect:** If the row is a phosphoprotein measurement, this column can contain the effect of the related phosphorylation on the protein activity. Please use `a` for activating and `i` for inhibiting phosphorylations. If the effect is too complex for a simple classification, then you can leave it blank (preferred) or use a `c` for the complex effect. But `c` will remove this row from possible causes, as CausalPath cannot evaluate complex effects. If this column is left blank for a row, then CausalPath looks it up from its database, which we compiled from PhosphoSitePlus and other resources.

**Value:** The numeric value for the row. There can be more than one value columns, but of course each of them with a unique name. There are many ways to encode values in the data file. They may represent normalized protein reads, or they can be comparison values like fold changes. The nature of the values has to be specified in the parameters file.

## Parameters file

The name of the parameters file have to be `parameters.txt` exactly. Each parameter in this file should be given in a separate line, in the format `parameter-name = parameter-value`. Below are list of possible parameters and their description.

`proteomics-values-file`: Proteomics values file. Path to the proteomics values file. It should have at least one ID column and one or more columns for experiment values. Platform file and values file can be the same file.

`proteomics-repeat-values-file`: Proteomics repeat values file. Path to the proteomics values file for the repeated experiment, if exists. This file has to have the exact same columns with the original proteomic values file, in the same order. Row IDs have to match with the corresponding row in the original data.

`proteomics-platform-file`: Proteomics platform file. Path to the proteomics platform file. Each row should belong to either a gene's total protein measurement, or a site specific measurement. This file should contain ID, gene symbols, modification sites, and known site effects. Platform file and values file can be the same file.

`id-column`: ID column in data file. The name of the ID column in platform or values files.

`symbols-column`: Symbols column in data file. The name of the symbols column in platform or values file.

`sites-column`: Sites column in data file. The name of the sites column.

`feature-column`: Feature column in data file. This optional column in the data file allows integration of multiple data types into a single input file. Make sure that each row has a unique ID, then use P for phosphopeptide, G for global protein, R for RNA, A for acetylpeptide, and M for methylpeptide.

`effect-column`: Site effect column in data file. The name of the effect column in platform or values file.

`value-transformation`: How to use values in the analysis. This parameter determines how to use the values in the proteomics file. Options are listed below. When there is only one value column and no transformation is desired, users can select any of the first 3 options, as they have no effect on a single value.

	as-is-single-value: This option should be selected when no transformation is desired, and there is a single value for each data row. Thresholding should be done using threshold-for-data-significance.

	arithmetic-mean: The arithmetic mean value of the given values is used for significance detection of a single change. There should only be one group of values (marked with value-column), the values have to be distributed around zero, and a threshold value should be provided for significance detection, using the threshold-for-data-significance.

	geometric-mean: The geometric mean value of the given values is used for significance detection of a single change. This is the only case when the geometric mean is used for averaging a group of samples, and it is appropriate if the individual values are formed of some kind of ratios. There should only be one group of values (marked with value-column), the values have to be distributed around zero, and a threshold value should be provided for significance detection, using the threshold-for-data-significance.

	max: The value with maximum absolute is used for the analysis. There should only be one group of values (marked with value-column), the values have to be distributed around zero, and a threshold value should be provided for significance detection, using the threshold-for-data-significance.

	difference-of-means: There should be control and test values, whose difference would be used for significance detection. The threshold for significance (threshold-for-data-significance) should also be provided.

	fold-change-of-mean: There should be control and test values, whose ratio will be converted to fold change and thresholded. The fold change value will be in the range (-inf, -1] + [1, inf). If the data file already contains a fold-change value, then please use the geometric-mean as value transformation. The threshold for significance (threshold-for-data-significance) should also be provided.

	significant-change-of-mean: There should be sufficient amount of control and test values to detect the significance of change with a t-test. Technically there should be more than 3 controls and 3 tests, practically, they should be much more to provide statistical power. The threshold-for-data-significance should be used for a p-value threshold, or alternatively, fdr-threshold-for-data-significance should be used for controlling significance at the false discovery rate level.

	significant-change-of-mean-paired: There should be sufficient amount of control and test values to detect the significance of change with a paired t-test. Technically there should be at least 3 controls and 3 tests, practically, they should be much more to provide statistical power. The order of control and test value columns indicate the pairing. First control column in the parameters file is paired with first test column, second is paired with second, etc. The threshold-for-data-significance should be used for a p-value threshold, or alternatively, fdr-threshold-for-data-significance should be used for controlling significance at the false discovery rate level.

	signed-p-values: If the dataset has its own calculation of p-values desired to be used directly, then for each comparison, there must be a column in the dataset that has these p-values multiplied with the sign of the change. For instance a value -0.001 means downregulation with a p-value of 0.001. Don't use 0 and -0 values in the data file with this option. Java cannot distinguish between the two. Instead, convert 0 values to very small but nonzero values, such as 1e-10 or -1e-10. If an FDR control is desired, there are two ways to have it. First way is to use unadjusted p-values in the data file and then use the fdr-threshold-for-data-significance parameter to set the desired FDR level. Second way is to use adjusted p-values in the data file and use the threshold-for-data-significance parameter to set the FDR level. Don't mix these two ways. If fdr-threshold-for-data-significance is used over already-adjusted p-values, then the code will apply the Benjamini-Hochberg procedure over those already-adjusted p-values.

	correlation: There should be one group of values (marked with value-column). There must be at least 3 value columns technically, but many more than that practically to have some statistical power for significant correlation. 



`value-column`: Value column in the data file. Name of a value column in the values file. This parameter should be used when there is only one group of experiments to consider in the analysis.

`control-value-column`: Control value column in the data file. Name of a control value column. This parameter should be used when there are control and test value columns in the dataset.

`test-value-column`: Test value column in the data file. Name of a test value column. This parameter should be used when there are control and test value columns in the dataset.

`do-log-transform`: Log transform data values. Whether the proteomic values should be log transformed for the analysis. Possible values are 'true' and 'false'. Default is false.

`rna-expression-file`: RNA expression file. Name of the RNA expression file. Simple tab-delimited text file where the first row has sample names, every other row corresponds to a gene, and first column is the gene symbol.

`acetyl-proteomic-file`: Acetylprotein readouts file. Name of the acetylpeptide-enriched proteomic file. The format of this file should be exactly the same with the phosphoproteomic file.

`methyl-proteomic-file`: Methylprotein readouts file. Name of the methylpeptide-enriched proteomic file. The format of this file should be exactly the same with the phosphoproteomic file.

`threshold-for-data-significance`: Threshold value for significant data. A threshold value for selecting significant data. Use this parameter only when FDR controlling procedure is already performed outside of CausalPath. This parameter can be set for each different data type separately. The parameter value has to be in the form 'thr-val data-type', such like '1 phosphoprotein' or '2 protein.

`fdr-threshold-for-data-significance`: FDR threshold for data significance. False discovery rate threshold for data significance. This parameter can be set for each different data type separately. The parameter value has to be in the form 'fdr-val data-type', such like '0.1 phosphoprotein' or '0.05 protein.

`pool-proteomics-for-fdr-adjustment`: Pool proteomics data for FDR adjustment. Whether to consider proteomic and phosphoproteomic data as a single dataset during FDR adjustment. This is typically the case with RPPA data, and typically not the case with mass spectrometry data. Can be 'true' or 'false'. Default is false.

`correlation-value-threshold`: Threshold for correlation value. Option to control correlation with its value. This cannot be used with FDR control, but can be used with p-value control.

`correlation-upper-threshold`: An upper threshold for correlation value. In some types of proteomic data, highest correlations come from errors. A way around is filtering with an upper value.

`pval-threshold-for-correlation`: P-value threshold for correlation. A p-value threshold for correlation in a correlation-based causality. This parameter should only be used when FDR control is performed outside of CausalPath.

`fdr-threshold-for-correlation`: FDR threshold for correlation. False discovery rate threshold for the correlations in a correlation-based analysis.

`stdev-threshold-for-data`: Standard deviation threshold for data. This parameter can be set for each different data type separately. The parameter value has to be in the form 'stdev-thr data-type', such like '0.5 phosphoprotein'.

`default-missing-value`: Default missing value in proteomics file. An option to specify a default value for the missing values in the proteomics file.

`minimum-sample-size`: Minimum sample size. When there are missing values in proteomic file, the comparisons can have different sample sizes for controls and tests. This parameter sets the minimum sample size of the control and test sets.

`calculate-network-significance`: Calculate network significance. Whether to calculate significances of the properties of the graph. When turned on, a p-value for network size, and also downstream activity enrichment p-values for each gene on the graph are calculated. This parameter is ignored by webserver due to resource limitations. To calculate network significance, please run CausalPath locally from its JAR file.

`permutations-for-significance`: Number of permutations for calculating network significance. We will do data randomization to see if the result network is large, or any protein's downstream is enriched. This parameter indicates the number of randomizations we should perform. It should be reasonable high, such as 1000, but not too high.

`fdr-threshold-for-network-significance`: FDR threshold for network significance. The false discovery rate for network significance calculations for the downstream activity enrichment of genes.

`use-network-significance-for-causal-reasoning`: Use network significance for causal reasoning. After calculation of network significances in a non-correlation-based analysis, this option introduces the detected active and inactive proteins as data to be used in the analysis. This applies only to the proteins that already have a changed data on them, and have no previous activity data associated.

`prioritize-activity-data`: Prioritize activity data over proteomic data for evidence of activity change. When there is an ActivityData associated to a protein (can be user hypothesis or inferred by network significance), do not use  other omic data for evidence of activity change in causal reasoning.

`minimum-potential-targets-to-consider-for-downstream-significance`: Minimum potential targets to calculate network significance. While calculating downstream significance for each source gene, we may not like to include those genes with just a few qualifying targets to reduce the number of tested hypotheses. These genes may not be significant even all their targets are in the results, and since we use Benjamini-Hochberg procedure to control the false discovery rate from multiple hypothesis testing, their presence will hurt the statistical power. Use this parameter to exclude genes with few qualifying targets on the network. Default is 5.

`do-site-matching`: Do site matching. Whether to force site matching in causality analysis. True by default.

`site-match-proximity-threshold`: Site-match proximity threshold. Phosphorylation relations many times know the target sites. When we observe a change in a site of the target protein which is not targeted by the relation, but the site is very close to a known target site of the relation, this parameter let's us to assume that the relation also applies to those close-by sites.

`site-effect-proximity-threshold`: Site-effect proximity threshold. CausalPath has a database of phosphorylation site effects. When not set, this parameter is 0 by default, which means exact usage of site effects. But sometimes we may see a site with unknown effect is modified, which is very close to another site with a known effect. This parameter let's us to assume those changing sites with unknown effect has the same effect with the neighbor site with known effect. Use responsibly.

`built-in-network-resource-selection`: Built-in network resources to use. Determines which network resource to use during the analysis. Multiple network resource should be mentioned together, separated with a space or comma. Possible values are below.

	PC: Pathway Commons v9 for all kinds of relations.

	PhosphoSitePlus: PhosphoSitePlus database for (de)phosphorylations, (de)acetylations and (de)methylations.

	REACH: Network derived from REACH NLP extraction results for phosphorylation relations.

	PhosphoNetworks: The PhosphoNetworks database for phosphorylations.

	IPTMNet: The IPTMNet database for phosphorylations.

	RHOGEF: The experimental Rho - GEF relations.

	PCTCGAConsensus: Unsigned PC relations whose signs are inferred by TCGA studies.

	TRRUST: The TRRUST database for expression relations.

	TFactS: The TFactS database for expression relations.

	NetworKIN: The NetworKIN database for phosphorylation relations.

	PCMetabolic: Relations involving chemicals in PC



`relation-filter-type`: Network relation-type filter. Use this parameter to limit the results with a specific type of relation. Possible values are below.

	no-filter: The graph is used with all inferred relations.

	phospho-only: Only phosphorylation relations are desired.

	site-specific-only: Only site-specific relations are desired.

	expression-only: Only expression relations are desired.

	without-expression: Everything but expression relations are desired.

	phospho-primary-expression-secondary: All phosphorylation relations are desired. Expression relations are desired only as supplemental, i.e., they have to involve at least one protein that exists in phospho graph.



`gene-focus`: Gene to focus. Use this parameter to crop the result network to the neighborhood of certain gene. You should provide gene symbols of these genes in a row separated by a semicolon, such like 'MTOR;RPS6KB1;RPS6'

`mutation-effect-file`: Mutation effect file. When we have mutations in the analysis, users can provide mutation effects using this parameter, otherwise all mutations are assumed to be inactivating.

`color-saturation-value`: Node color saturation value. Specifies the value where node colors reach most intense color. Has to be a positive value, and used symmetrically. In the case of value-transformation is significant-change-of-mean, the value is -log(p) with a sign associated to it.

`show-all-genes-with-proteomic-data`: Show all genes with significant proteomic data. CausalPath generates a result graph, but what about all other significant changes that could not make into the network? CausalPath puts those genes as disconnected nodes in the graph when the analysis is not correlation based. This is true by default but can be turned off by setting to false.

`show-insignificant-data`: Show insignificant proteomic data on the graph. Option to make the insignificant protein data on the result graph visible. Seeing these is good for seeing what is being measured, but when they are too much, turning off generates a a better view.

`hide-data-not-part-of-causal-relations`: Hide data which did not contribute causal relations. Limits the data drawn on the result graph to the ones that take part in the identified causal relations.

`data-type-for-expressional-targets`: Data type explainable by an expressional relation. By default, CausalPath generates explanations only for proteomic changes. But it is possible to explain RNA changes with expressional relations as well, and it is a more direct explanation than total protein measurement. This parameter lets users to control possible data types explainable by expressional relations. Typical values are 'rna' and 'protein'. This parameter can also  be used multiple times to use rna and protein data together.

`generate-data-centric-graph`: Generate a data-centric view as well. An alternative to the gene-centric graph of CausalPath is a data-centric graph where nodes are not genes but the data. This parameter forces to generate this type of result as well. False by default.

`gene-activity`: Gene activity hypotheses to include. Use this parameter to assign a specific activity or inactivity to a gene in the analysis. The value has to start with a gene name and one letter code for activity or inactivity, such as 'BRAF a', or 'PTEN i'.

`tf-activity-file`: Transcription factor activity inference file. CausalPath lets users to input results from an inference for transcriptional factor activities, such as PRECEPTS, MARINa or VIPER. For this, the results should be prepared in a file, first column containing TF symbol and the second column whether 'activated' or 'inhibited'. The name of such file should be provided here.

`use-strongest-proteomic-data-per-gene`: Use strongest proteomic data per gene. When a proteomic experiment outputs too many phosphorylation sites with lots of changes, many proteins have evidences for both activation and inhibition. This produces networks hard to read. A complexity management technique is to turn on this parameter to use only the strongest proteomic feature at the upstream of relations. This is false by default.

`use-missing-proteomic-data-for-test`: Include missing proteomic data in tests. Option to use a G-test to check unequal distribution of missing values. If opted, and data is sufficient, the G-test result is combined with t-test result with Fisher's method. But beware. This method assumes that missing values are uniformly distributed to samples. If this is violated, then false positives will appear. If you are not sure, stay away from this option.

`randomized-matrix-directory-for-missing-proteomic-data`: Directory name for randomized boolean matrices for missing data distribution. Using randomization is an alternative to using a G-test for interpreting missing data distribution. CausalPath cannot generate those matrices, but it can use pre-generated matrices to compute significances. This operation typically requires a lot of memory.

`missing-value-test-data-sufficiency-threshold`: Missing value test data sufficiency threshold. When we use a G-test in the analysis, we don't want to use it for every proteomic row. Some rows will have insufficient data. To test for sufficiency, we generate an extreme case where missing data is shifted to the smaller group and see if this can provide a p-value small enough. If this extreme shifting cannot make the the p-value small enough (specified with this threshold), then we don't use a G-test for that row.

`custom-resource-directory`: Custom resource directory name. CausalPath downloads some data in the first run and stores in the resource directory. This directory is '.panda' by default. If this needs to be customized, use this parameter.

`tcga-directory`: TCGA data directory. It is possible to add genomic data from TCGA to CausalPath analysis. This is only useful when the proteomic data have the same sample IDs. Users can load TCGA data into a local directory from Broad Firehose, and provide the directory here. The org.panda.resource.tcga.BroadDownloader in the project https://github.com/PathwayAndDataAnalysis/resource is a utility that can do that.

`hgnc-file`: HGNC data file. For reproducibility: Provide an HGNC resource file to reproduce a previous analysis.

`custom-causal-priors-file`: Custom causal priors file. For reproducibility: Provide a custom file for causal priors.

`custom-site-effects-file`: Custom site effects file. For reproducibility: Provide a custom file for site effects.

`use-expression-for-activity-evidence`: Experimental parameter. For testing if RNA expression is a good proxy for protein activity.
