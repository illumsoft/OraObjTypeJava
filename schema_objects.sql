whenever sqlerror exit sql.sqlcode rollback
whenever oserror exit failure rollback

create type ora_type
as object(
  n number(3,0),
  v varchar2(10),
  d date
)
/

create table ora_table(
  n number(3,0) constraint ora_table_pk primary key,
  v varchar2(10),
  d date
)
/

create function ora_func_set(p ora_type)
  return integer
as
begin
  insert into ora_table(n, v, d)
  values (p.n, p.v, p.d);

  return p.n;
end ora_func_set;
/

create function ora_func_get(p integer)
  return ora_type
as
  ret ora_type;
begin
  select ora_type(n, v, d)
  into ret
  from ora_table;

  return ret;
end ora_func_get;
/

