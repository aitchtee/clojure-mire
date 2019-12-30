%
% SWI Prolog mire bot (c) Sergey Sinitsa 2017.
%
:- use_module(library(socket)).

:- dynamic exit/1.
:- dynamic emoji/1.

e(north) --> [north].
e(south) --> [south].
e(west) --> [west].
e(east) --> [east].
anything --> [].
anything --> [_], anything.
exits([Exit]) --> e(Exit).
exits([Exit|Exits]) --> e(Exit), exits(Exits).
parse_exits(Exits) --> [exits], exits(Exits), ['.'].
parse_end_line --> [maniacs] , anything, ['.'].
parse_end_line --> anything, [lock] , anything.
parse_gen_maniac(Emoji) --> anything, [you],[gen], [maniac], [with], [emotion],  [Emoji], ['.'].

parse_emoji(Tokens):- phrase(parse_gen_maniac(Emoji),Tokens,Rest),retractall(emoji(_)),assert(emoji(Emoji)).
%parse_emoji(_).

parse_exit(Tokens) :- phrase(parse_exits(Exits), Tokens, Rest), retractall(exit(_)), assert(exit(Exits)).
parse_exit(_).

/* Convert to lower case if necessary,
skips some characters,
works with non latin characters in SWI Prolog. */
filter_codes([], []):-!.
filter_codes([H|T1], T2) :-
  char_code(C, H),
  member(C, ['(', ')', ':','#','{','}']),!,
  filter_codes(T1, T2).
filter_codes([H|T1], [F|T2]) :-
  code_type(F, to_lower(H)),
  filter_codes(T1, T2).


halprocess(Stream) :-
  exit(Directions),
  random_member(Direction,Directions),
  format(atom(Command), 'move ~w~n', [Direction]),
  write(Command),
  write(Stream, Command),
  flush_output(Stream),!.

halprocess(_):-write('I am stuck').

tell_name(Stream,[what,is,your,name,?]):-
    !,format(atom(Name), '~w~n', [dupel_bot]),
  write(Name),
  write(Stream,Name),
  flush_output(Stream).


endSimb(Tokens):-phrase(parse_end_line,Tokens,Rest).
process(_,_).

readLoop(Stream,Tokens):-
  read_line_to_codes(Stream, Codes),
  filter_codes(Codes, Filtered),
  atom_codes(Atom, Filtered),
  tokenize_atom(Atom, Tokens).


loop(Stream) :-
  %Set name
  repeat,
  readLoop(Stream,Tokens),
  (tell_name(Stream,Tokens),!;fail),
  write(Tokens),
  flush_output(Stream),
  flush(),
  %Waiting for input and parse exits
  repeat,
  readLoop(Stream,Tokens0),
  parse_exit(Tokens0),
  write(Tokens0),
  (endSimb(Tokens0),!;fail),
  %Gen maniac
  format(atom(CommandGenMan), 'gen-maniac ~n', []),
  write(Stream,CommandGenMan),
  write(CommandGenMan),
  flush_output(Stream),
  %Get emoji
  repeat,
  readLoop(Stream,Tokens1),
  writeln(Tokens1),
  (parse_emoji(Tokens1),!;fail),
  findall(X,emoji(X),Res),
  write(Res),
  %Set emoji
  member(Y,Res),
  format(atom(Emoji), 'set_emoji ~w~nlook ~n', [Y]),
  write(Stream,Emoji),
  write(Emoji),
  flush_output(Stream),

  repeat,
  readLoop(Stream,Tokens2),
  parse_exit(Tokens2),sleep(1),
  writeln(Tokens2),
  flush(),
   (endSimb(Tokens2),
    halprocess(Stream),sleep(1);true),
  fail.

 main :-
   setup_call_cleanup(
     tcp_connect(localhost:3335, Stream, []),
     loop(Stream),
     close(Stream)).

