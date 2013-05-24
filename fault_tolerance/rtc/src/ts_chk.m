%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% RTC (settle time computation) 
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%  The unit transition:
%  Based on the avgP:uReboot ratio, e.g, 500->10ms/20us, 5000->100ms/20us
%  The tasks set used here has avgP 200. It can be ms or us, as long as 
%  the urebbot is calculated based on the same ratio!!
%
%  E.g, 500->200/0.4, 5000->200/0.04 (can  be us or ms)
%
%  The scale and fac are used to do this, just looks weired
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

path = '../../../../tasks/workset/';

%ratio = [500 5000 50000];   % avgP : ureboot
ratio = [10];

%mode = 'eager';             %eager or lazy (ondemand)
mode = 'checkpoint';             %eager or lazy (ondemand)
%wkset = 1:1:30;               % 30 schedulable workset
wkset = 1:1:30;               % 30 schedulable workset
util = 20:10:90;           % total util
thd_num = 20:1:20;         % total thread numbers 20, 50, 1000

objNum = 5:10:5;        % number of objects to be recovered(per task)

chk_rcost = 0;
chk_scost = 0;

chkP = 500; % avgP of task is 200, and chkP is chosen 500 for now

% objects to be recovered
for p =1:length(objNum)
    % ratio
    for i = 1:length(ratio)
        fac = 1/ratio(i)*10000;  % see above explaination for ureboot ratio 
        scale = fac; 

        %%%%%%%%%%%%%%
        % CheckPoint (ureboot ratio does not matter)
        % based on the ureboot ratio, choose us as the unit here
        if (strcmp(mode,'checkpoint'))
            objNum = 1:1:1;    % does not matter for checkpoint
            chk_rcost = 200/ratio(i);
            % treat the checkpointing process as an additional process with its own
            % prio, so this will be used for demand. For now...
            chk_scost = chk_rcost; % for Composite model, 1:1. Linux/VM will have different ratio
        end
        %%%%%%%%%%%%%%
        
        % thread numbers
        for j = 1:length(thd_num)
            thdStr = num2str(thd_num(j));
            %total utilization
            for k = 1:1:length(util)
                % ratio is 4:1 according to the measurment (1 object)
                micro_reboot = 0.02*scale;   % 20us as a constant
                if (strcmp(mode,'checkpoint'))
                    micro_reboot = micro_reboot*1000;  % for checkpoint, we use us
                end
                obj_recovery = micro_reboot/4;
                
                utilStr = num2str(util(k));
                utilStr = strcat(strcat(thdStr,'/'), strcat(utilStr,'/'));
                final_path = strcat(path, utilStr);
                saved_dir = strcat(num2str(thd_num(j)), num2str(util(k)));
                saved_dir = strcat(mode, saved_dir);
                saved_dir = strcat(num2str(ratio(i)), saved_dir);
                saved_dir = strcat(saved_dir, '_obj');
                saved_dir = strcat(saved_dir, num2str(objNum(p)));    
                saved_dir = strcat(strcat(num2str(util(k)),'/'), saved_dir);  
                saved_dir = strcat(strcat(num2str(ratio(i)),'/'), saved_dir);  
                saved_dir = strcat(strcat(num2str(thd_num(j)),'/'), saved_dir);  
                mkdir(saved_dir);
                for m = 1:length(wkset)
                    if (wkset(m) == 11)
                        continue;
                    end
                    
                    wkStr = num2str(wkset(m));
                    target = wkStr;
                    
                    wkStr = strcat(wkStr, '_util_');
                    wkStr = strcat(wkStr, num2str(util(k)));
                    wkStr = strcat(wkStr,'.0.data');
                    
                    finalFile = strcat(final_path, wkStr);
                    fprintf('%s \n', finalFile);
                    %current work set info
                    fprintf('<<< %s: ', mode);
                    fprintf('tasks %s ', num2str(thd_num(j)));
                    fprintf('utils %s ', num2str(util(k)));
                    fprintf('objec %s ', num2str(objNum(p)));
                    fprintf('ratio %s >>>\n', num2str(ratio(i)));

                    % settle time computation
                    [a0 b0 Ts Pos] = ts_script(finalFile, micro_reboot, obj_recovery, mode, objNum(p), chk_rcost, chk_scost, chkP);
                    
                    if (Pos == -55)
                        fprintf('File %s not found\n', finalFile);
                        continue;
                    end
                    
                    if (Pos == -99)
                        fprintf('Task set is not found\n');
                        continue;
                    end
                    
                    test = strcat(saved_dir, '/');
                    test = strcat(test, target);
                    fclose(fopen(test, 'w'));
                    
                    dlmwrite(test, [util(k) Ts], ...
                        '-append', 'precision', '%.4f', 'delimiter', ' ')
                    
                end
            end
        end
    end
end

