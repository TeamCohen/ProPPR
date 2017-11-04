# Semi-Supervised Learning with ProPPR

This example shows how to use ProPPR for semi-supervised learning task - text categorization. 

There are two parts in this example: 
1. Run ProPPR with fixed number of examples on different datasets: citeseer, cora, pubmed
2. Tune hyper-parameter with Bayesian Optimization using Spearmint

## Part 1: ProPPR for TextCat-SSL
First, let's run ProPPR with some fixed parameters. 
1. Clone the [ProPPR](https://github.com/TeamCohen/ProPPR) project.
2. Set the path of ProPPR in 'expt-getoor.bash'. You can also change other settings, such as dataset, eps, depth, etc.
3. Run '$ ./expt-getoor.bash'
4. You can change the number of unlabelled examples in 'settings.in'. Refer to this [paper](https://arxiv.org/abs/1703.01557v2) for more information.


## Part 2: Tuning Hyper-Parameters
Now, we will run Spearmint to find the optimal number of unlabelled data, which are used as constraints in this example.
1. Install [Spearmint](https://github.com/HIPS/Spearmint)
2. Change the path of ProPPR and Spearmint in 'expt-getoor-spearmint.bash'
3. Run '$ ./expt-getoor-spearmint.bash'
4. You can find the tuning progress in 'spearmint.log'
