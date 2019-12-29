:- use_module(library(socket)).

:- dynamic exit/1.
:- dynamic item/1.

e(north) --> [north].
e(south) --> [south].
e(west) --> [west].
e(east) --> [east].
exits([Exit]) --> e(Exit).
exits([Exit|Exits]) --> e(Exit), exits(Exits).
parse_exits(Exits) --> [exits], exits(Exits), ['.'].

i(keys) --> [keys].
i(turtle) --> [turtle].
items([Item]) --> i(Item).
items([Item|Items]) --> i(Item), items(Items).
parse_items(Items) --> [items], items(Items), ['.'].



parse(Tokens) :- phrase(parse_exits(Exits), Tokens, Rest),
                 retractall(exit(_)),
                 assert(exit(Exits)).
parse(_).

parse1(Tokens) :- phrase(parse_exits(Exits), Tokens, Rest).
parse1(_).


parse2(Items) :-  phrase(parse_items(Items), Items, Rest),
                 retractall(item(_)),
                 assert(item(Items)).
parse2(_).
/* Convert to lower case if necessary,
skips some characters,
works with non latin characters in SWI Prolog. */
filter_codes([], []).
filter_codes([H|T1], T2) :-
  char_code(C, H),
  member(C, ['(', ')', ':']),
  filter_codes(T1, T2).
filter_codes([H|T1], [F|T2]) :-
  code_type(F, to_lower(H)),
  filter_codes(T1, T2).

process2(Stream) :-
  item([Direction|_]),
  format(atom(Command), 'grab ~w~n', [Direction]),
  write(Command),
  write(Stream, Command),
  flush_output(Stream),
  retractall(item(_)).
process2(_).

checkq(Stream,[_|?]):-
  !,format(atom(Name), '~w~n', [a]),
  write(Name),
  write(Stream,Name),
  flush_output(Stream).


process(Stream) :-
  exit([Direction|_]),
  format(atom(Command), 'move ~w~n', [Direction]),
  write(Command),
  write(Stream, Command),
  flush_output(Stream),
  retractall(exit(_)).
process(_).

process1(Stream) :-
  item([Direction|_]),
  write(Direction),nl,
  format(atom(Command), 'grab ~w~n', [Direction]),
  write(Command),
  write(Stream, Command),
  flush_output(Stream),
  retractall(item(_)).
process1(_).


processw(Stream) :-
  format(atom(Command), 'work ~w~n', ['']),
  write(Command),
  write(Stream, Command),
  flush_output(Stream).

processw(_).

checkq1(Stream,[>,how,many,colours,in,rainbow,?,?,a,-7,,,b,-5,,,c,-6]):-
  !,format(atom(Name), '~w~n', [a]),
  write(Name),
  write(Stream,Name),
  flush_output(Stream).
checkq1(_,_).

checkq2(Stream,[>,a,-17,,,b,-12,,,c,-22]):-
  !,format(atom(Name), '~w~n', [c]),
  write(Name),
  write(Stream,Name),
  flush_output(Stream).
checkq2(_,_).



checkName(Stream,[what,is,your,name,?]):-
  !,format(atom(Name), '~w~n', [dupel]),
  write(Name),
  write(Stream,Name),
  flush_output(Stream).

checkName(_,_).

loop(Stream) :-
loop0(Stream,Tokens),
  checkName(Stream,Tokens),

  parse(Tokens),
  nl,
  flush(),
  sleep(1),
  process(Stream),
  loop1(Stream).

loop0(Stream, Tokens):-  read_line_to_codes(Stream, Codes),
  filter_codes(Codes, Filtered),
  atom_codes(Atom, Filtered),
  tokenize_atom(Atom, Tokens),
  write(Tokens),parse(Tokens).



loop1(Stream) :-
  loop0(Stream,Tokens),
  checkName(Stream,Tokens),


  processw(Stream),
  loop0(Stream,Tokens),
  write(Tokens),parse(Tokens),
  checkq1(Stream,Tokens),

 loop0(Stream,Tokens),
  write(Tokens),parse(Tokens),
  checkq2(Stream,Tokens),

 loop0(Stream,Tokens), loop1(Stream).

main :-
 setup_call_cleanup(
     tcp_connect(localhost:3335, Stream, []),
     loop(Stream),
     close(Stream)).
