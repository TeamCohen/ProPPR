%%%%%%%%%%%%%%%%%%%% a test case %%%%%%%%%%%%%%%%%%%%
% this works for either the swi-prolog or the 
% tuProlog version of outlinks

% declarations
propprDB sister(_,_).
propprDB spouse(_,_),child(_,_).
propprRule sim(_,_),rel(_,_),rel2(_,_).

% a ruleset
sim(X,X) :- true # base.
sim(X,Y) :- sister(X,Z), sim(Z,Y) # sister.
sim(X,Y) :- child(X,Z), sim(Z,Y) # child.
% sim(X,Y) :- spouse(X,Z), sim(Z,Y) # spouse(X).

rel(X,Y) :- sister(X,Y).
rel(X,Y) :- child(X,Y).
rel(X,Y) :- spouse(X,Y).

rel2(X,Y) :- rel(X,Z), rel(Z,Y).

% a database

sister(william,rachel).
sister(william,sarah).
sister(william,lottie).

spouse(william,susan).
spouse(susan,william).

child(lottie,charlotte).
child(lottie,lucas).
child(sarah,poppy).
child(rachel,caroline).
child(rachel,elizabeth).

